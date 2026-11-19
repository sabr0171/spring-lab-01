package kz.iitu.spring_lab_01.fine;

import kz.iitu.spring_lab_01.catalog.Book;
import kz.iitu.spring_lab_01.catalog.BookNotFoundException;
import kz.iitu.spring_lab_01.catalog.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Lab 12, individual variant 6: the class under unit test (FineServiceTest).
// A fine is days overdue x the daily rate — capped at the maximum, however late the book comes back.
@Service
@Transactional(readOnly = true)
public class FineService {

    private final FineRepository fines;
    private final BookRepository books;
    private final FinePolicy policy;

    public FineService(FineRepository fines, BookRepository books, FinePolicy policy) {
        this.fines = fines;
        this.books = books;
        this.policy = policy;
    }

    @Transactional
    public Fine charge(long bookId, String reader, int daysOverdue) {
        if (daysOverdue < 1) {
            throw new IllegalArgumentException("A fine needs at least one day overdue, got " + daysOverdue);
        }
        Book book = books.findById(bookId).orElseThrow(() -> new BookNotFoundException(bookId));
        return fines.save(new Fine(book, reader, daysOverdue, amountFor(daysOverdue)));
    }

    // long: 30 000 days at 100 tenge would not overflow an int, but a misconfigured rate could
    int amountFor(int daysOverdue) {
        long amount = (long) daysOverdue * policy.dailyRate();
        if (amount > policy.maximum()) {
            return policy.maximum();   // variant 6: the cap
        }
        return (int) amount;
    }

    public List<Fine> unpaidOf(String reader) {
        return fines.findByReaderAndPaidFalseOrderByAmountDescIdAsc(reader);
    }

    public long totalOwedBy(String reader) {
        return fines.totalUnpaid(reader);
    }
}
