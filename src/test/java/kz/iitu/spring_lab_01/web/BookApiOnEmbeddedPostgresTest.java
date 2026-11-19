package kz.iitu.spring_lab_01.web;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;

// The tests of LibraryApiScenario on an embedded PostgreSQL 17 (io.zonky binaries started from the test JVM) —
// what this computer, which has no Docker, can run instead of BookApiIntegrationTest. It is NOT Testcontainers:
// no image, no container, no @ServiceConnection. And it is not H2: nothing in this project uses H2.
// The database is its own, empty at the start like a fresh container: these tests commit, and the books and
// fines they leave must not reach the shared database of the other test classes.
class BookApiOnEmbeddedPostgresTest extends LibraryApiScenario {

    static final EmbeddedPostgres DATABASE = startDatabase();

    static EmbeddedPostgres startDatabase() {
        try {
            return EmbeddedPostgres.builder()
                    .setServerConfig("lc_messages", "C")
                    .start();   // a JVM shutdown hook stops it
        } catch (IOException e) {
            throw new UncheckedIOException("The embedded PostgreSQL did not start", e);
        }
    }

    @DynamicPropertySource
    static void dataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> DATABASE.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }
}
