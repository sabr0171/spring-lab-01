package kz.iitu.spring_lab_01.catalog;

import kz.iitu.spring_lab_01.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

// Task 3. Only the data layer is started: entities, repositories, Flyway, Hibernate. Every test is one
// transaction, rolled back at the end, so V2's three books are all that is there when a test begins.
// replace = NONE: no H2 is swapped in — there is none on the classpath. The slice runs on the embedded
// PostgreSQL 17 of PostgresIntegrationTest, migrated by the same Flyway scripts as production.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookRepositoryTest extends PostgresIntegrationTest {

    @Autowired
    BookRepository repository;

    @Autowired
    TestEntityManager em;

    @Test
    void findsEveryBookOfTheAuthorWhateverTheCaseAndPartOfTheName() {
        Book ddd = em.persist(new Book("Domain-Driven Design", "Eric Evans", Genre.PROGRAMMING, 2003));
        Book reference = em.persist(new Book("Domain-Driven Design Reference", "Eric Evans", Genre.PROGRAMMING, 2014));
        em.flush();
        em.clear();          // the persistence context is empty: the answer has to come from SQL

        var found = repository.findByAuthorContainingIgnoreCaseOrderByIdAsc("EVANS");

        assertThat(found).extracting(Book::getId).containsExactly(ddd.getId(), reference.getId());
        assertThat(found).extracting(Book::getTitle)
                .containsExactly("Domain-Driven Design", "Domain-Driven Design Reference");
    }

    // The negative case: "Robert" must not bring Martin Fowler or a book with no author at all
    @Test
    void leavesOutOtherAuthorsAndBooksThatHaveNoAuthor() {
        em.persist(new Book("Refactoring", "Martin Fowler", Genre.PROGRAMMING, 2018));
        Book anonymous = em.persist(new Book("The Epic of Gilgamesh", null, Genre.POETRY, 1200));
        em.flush();
        em.clear();

        assertThat(repository.findByAuthorContainingIgnoreCaseOrderByIdAsc("Robert"))
                .extracting(Book::getAuthor)
                .containsExactly("Robert Martin");                          // the seeded Clean Code only
        assertThat(repository.findByAuthorContainingIgnoreCaseOrderByIdAsc("Evans")).isEmpty();
        assertThat(em.find(Book.class, anonymous.getId())).isNotNull();   // it is there; the query skips it
    }
}
