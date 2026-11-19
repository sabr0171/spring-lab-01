package kz.iitu.spring_lab_01.fine;

import kz.iitu.spring_lab_01.catalog.Book;
import kz.iitu.spring_lab_01.catalog.BookNotFoundException;
import kz.iitu.spring_lab_01.catalog.BookRepository;
import kz.iitu.spring_lab_01.catalog.Genre;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Task 1, variant 6. A unit test: no Spring context and no database. The repositories are Mockito mocks,
// the policy is a plain record (100 tenge a day, at most 2000), and the service is created with new.
@ExtendWith(MockitoExtension.class)
class FineServiceTest {

    static final FinePolicy POLICY = new FinePolicy(100, 2000);

    @Mock
    FineRepository fines;

    @Mock
    BookRepository books;

    FineService service;
    Book refactoring;

    @BeforeEach
    void createService() {
        service = new FineService(fines, books, POLICY);
        refactoring = new Book("Refactoring", "Martin Fowler", Genre.PROGRAMMING, 2018);
    }

    // 1. Happy path: the book exists, the fine is days x rate, and the saved fine is what comes back
    @Test
    void threeDaysLateCostThreeTimesTheDailyRate() {
        when(books.findById(7L)).thenReturn(Optional.of(refactoring));
        when(fines.save(any(Fine.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Fine fine = service.charge(7L, "user", 3);

        assertThat(fine.getAmount()).isEqualTo(300);
        assertThat(fine.getDaysOverdue()).isEqualTo(3);
        assertThat(fine.getReader()).isEqualTo("user");
        assertThat(fine.isPaid()).isFalse();
    }

    // 2. Not found: Optional.empty() from the repository becomes BookNotFoundException.
    //    Variant 6 asks for assertThatThrownBy with an assertion on the message — here it is.
    @Test
    void aFineForABookThatDoesNotExistIsRefused() {
        when(books.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.charge(404L, "user", 3))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessage("Book 404 does not exist");
        verify(fines, never()).save(any());
    }

    // 3. Interaction: not what came back, but exactly what the service handed to the repository
    @Test
    void theRepositoryReceivesTheBookTheReaderTheDaysAndTheAmount() {
        when(books.findById(7L)).thenReturn(Optional.of(refactoring));

        service.charge(7L, "user", 5);

        ArgumentCaptor<Fine> captor = ArgumentCaptor.forClass(Fine.class);
        verify(fines).save(captor.capture());
        Fine saved = captor.getValue();
        assertThat(saved.getBook()).isSameAs(refactoring);
        assertThat(saved.getReader()).isEqualTo("user");
        assertThat(saved.getDaysOverdue()).isEqualTo(5);
        assertThat(saved.getAmount()).isEqualTo(500);
        assertThat(saved.isPaid()).isFalse();
    }

    // 4. Variant 6, the branch that is easy to forget: 45 days x 100 = 4500, but the fine stops at 2000
    @Test
    void aVeryLateBookIsFinedNoMoreThanTheMaximum() {
        when(books.findById(7L)).thenReturn(Optional.of(refactoring));

        service.charge(7L, "user", 45);

        ArgumentCaptor<Fine> captor = ArgumentCaptor.forClass(Fine.class);
        verify(fines).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualTo(POLICY.maximum());
        assertThat(captor.getValue().getDaysOverdue()).isEqualTo(45);   // the days are kept as they were
    }

    // Task 5: JaCoCo showed line 29 yellow, "1 of 2 branches missed" — if (daysOverdue < 1) had only ever been
    // false. Over HTTP it cannot be true (@Min(1) on the request answers 400 first), so only a unit test reaches it.
    @Test
    void zeroDaysOverdueIsRefusedBeforeAnythingIsLookedUp() {
        assertThatThrownBy(() -> service.charge(7L, "user", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A fine needs at least one day overdue, got 0");
        verifyNoInteractions(books, fines);
    }
}
