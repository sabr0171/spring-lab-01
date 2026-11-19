package kz.iitu.spring_lab_01.fine;

import kz.iitu.spring_lab_01.PostgresIntegrationTest;
import kz.iitu.spring_lab_01.catalog.Book;
import kz.iitu.spring_lab_01.catalog.Genre;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Task 3 for the variant's own table: what a reader still owes.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FineRepositoryTest extends PostgresIntegrationTest {

    @Autowired
    FineRepository repository;

    @Autowired
    TestEntityManager em;

    Book book;

    @BeforeEach
    void aBook() {
        book = em.persist(new Book("Refactoring", "Martin Fowler", Genre.PROGRAMMING, 2018));
    }

    Fine fine(String reader, int days, int amount, boolean paid) {
        Fine fine = new Fine(book, reader, days, amount);
        if (paid) {
            fine.markPaid();
        }
        return em.persist(fine);
    }

    @Test
    void listsTheUnpaidFinesOfAReaderBiggestFirst() {
        fine("user", 3, 300, false);
        fine("user", 45, 2000, false);
        fine("user", 5, 500, false);
        em.flush();
        em.clear();          // force the query to the database

        var unpaid = repository.findByReaderAndPaidFalseOrderByAmountDescIdAsc("user");

        assertThat(unpaid).extracting(Fine::getAmount).containsExactly(2000, 500, 300);
        assertThat(unpaid.getFirst().getBook().getTitle()).isEqualTo("Refactoring");   // fetched with the fine
    }

    // The negative case: a paid fine, and another reader's fine, are not what "user" owes
    @Test
    void leavesOutPaidFinesAndOtherReadersFines() {
        fine("user", 3, 300, false);
        fine("user", 10, 1000, true);
        fine("admin", 7, 700, false);
        em.flush();
        em.clear();

        assertThat(repository.findByReaderAndPaidFalseOrderByAmountDescIdAsc("user"))
                .singleElement()
                .extracting(Fine::getAmount)
                .isEqualTo(300);
        assertThat(repository.totalUnpaid("user")).isEqualTo(300);
        assertThat(repository.totalUnpaid("nobody")).isZero();   // coalesce: no rows is 0, not null
    }

    // The table itself refuses a fine without any day overdue (V6: check (days_overdue > 0))
    @Test
    void theDatabaseRefusesAFineWithNoDaysOverdue() {
        assertThatThrownBy(() -> em.persistAndFlush(new Fine(book, "user", 0, 100)))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("fine_days_overdue_check");
    }
}
