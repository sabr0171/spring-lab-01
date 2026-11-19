package kz.iitu.spring_lab_01.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

// Task 4. The application as a client sees it: a real Tomcat on a random port, both security filter chains,
// controllers, validation, services, transactions, Hibernate, Flyway, PostgreSQL. Nothing is mocked; every
// request commits. The same tests run twice:
//   BookApiIntegrationTest        - PostgreSQL in a Docker container (Testcontainers + @ServiceConnection)
//   BookApiOnEmbeddedPostgresTest - an embedded PostgreSQL, the stand-in on a computer without Docker
// Login: HTTP Basic as user/user123 and admin/admin123, the rows of V5__security.sql checked by BCrypt
// (lab 11's tokens are not on this branch). Every test makes its own book, so their order does not matter;
// only one test writes fines, so the totals it checks are its own.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
abstract class LibraryApiScenario {

    static final String ADMIN = "admin", ADMIN_PASSWORD = "admin123";
    static final String USER = "user", USER_PASSWORD = "user123";

    @Autowired
    RestTestClient client;

    URI newBook(String title, String author, int year) {
        URI location = client.post().uri("/api/books")
                .headers(h -> h.setBasicAuth(ADMIN, ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"title": "%s", "author": "%s", "genre": "PROGRAMMING", "year": %d}
                        """.formatted(title, author, year))
                .exchange()
                .expectStatus().isCreated()
                .returnResult()
                .getResponseHeaders().getLocation();
        assertThat(location).isNotNull();
        return location;
    }

    @Test
    void aBookCreatedByTheAdministratorIsReadBackWithItsCitation() {
        URI ddd = newBook("Domain-Driven Design", "Eric Evans", 2003);

        client.get().uri(ddd)
                .headers(h -> h.setBasicAuth(USER, USER_PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Domain-Driven Design")
                .jsonPath("$.year").isEqualTo(2003);

        client.get().uri(ddd + "/citation")
                .headers(h -> h.setBasicAuth(USER, USER_PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("Eric Evans. Domain-Driven Design. 2003.");
    }

    // Variant 6 end to end: V6's table, the cap, and a reader who sees only their own fines
    @Test
    void finesAreCappedAndEachReaderSeesOnlyTheirOwn() {
        URI book = newBook("Working Effectively with Legacy Code", "Michael Feathers", 2004);
        chargeFine(book, USER, 4);       // 400
        chargeFine(book, USER, 60);      // 6000 -> capped at 2000
        chargeFine(book, ADMIN, 2);      // 200, someone else's

        client.get().uri("/api/fines/mine")
                .headers(h -> h.setBasicAuth(USER, USER_PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.reader").isEqualTo(USER)
                .jsonPath("$.total").isEqualTo(2400)
                .jsonPath("$.fines.length()").isEqualTo(2)
                .jsonPath("$.fines[0].amount").isEqualTo(2000)
                .jsonPath("$.fines[0].daysOverdue").isEqualTo(60)
                .jsonPath("$.fines[0].bookTitle").isEqualTo("Working Effectively with Legacy Code")
                .jsonPath("$.fines[1].amount").isEqualTo(400);
    }

    void chargeFine(URI book, String reader, int days) {
        client.post().uri(book + "/fines")
                .headers(h -> h.setBasicAuth(ADMIN, ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"reader": "%s", "daysOverdue": %d}
                        """.formatted(reader, days))
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void aFineWithoutADayOverdueIsA400AndAFineForAMissingBookIsA404() {
        URI book = newBook("Release It!", "Michael Nygard", 2018);

        client.post().uri(book + "/fines")
                .headers(h -> h.setBasicAuth(ADMIN, ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"reader": "user", "daysOverdue": 0}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.invalidFields[0].field").isEqualTo("daysOverdue");

        client.post().uri("/api/books/999999/fines")
                .headers(h -> h.setBasicAuth(ADMIN, ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"reader": "user", "daysOverdue": 3}
                        """)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.detail").isEqualTo("Book 999999 does not exist");
    }

    @Test
    void strangersAreAskedToLogInAndReadersCannotFine() {
        client.get().uri("/api/books")
                .exchange()
                .expectStatus().isUnauthorized();

        client.get().uri("/api/fines/mine")
                .headers(h -> h.setBasicAuth(USER, "not-the-password"))
                .exchange()
                .expectStatus().isUnauthorized();

        client.post().uri("/api/books/1/fines")
                .headers(h -> h.setBasicAuth(USER, USER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"reader": "admin", "daysOverdue": 30}
                        """)
                .exchange()
                .expectStatus().isForbidden();
    }
}
