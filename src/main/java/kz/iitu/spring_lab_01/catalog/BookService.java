package kz.iitu.spring_lab_01.catalog;

import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class BookService {

    private static final Sort BY_ID = Sort.by("id");

    private final BookRepository repository;

    public BookService(BookRepository repository) {
        this.repository = repository;
    }

    // author == null -> all books; otherwise case-insensitive "contains" (so "Bloch" matches "Joshua Bloch")
    public List<Book> findAll(String author) {
        if (author == null || author.isBlank()) {
            return repository.findAll(BY_ID);
        }
        return repository.findByAuthorContainingIgnoreCaseOrderByIdAsc(author.strip());
    }

    public Optional<Book> findById(long id) {
        return repository.findById(id);
    }

    // Lab 12: for callers that need the book to exist — the JSON API turns the exception into a 404
    public Book requireById(long id) {
        return repository.findById(id).orElseThrow(() -> new BookNotFoundException(id));
    }

    // Lab 10, task 3.2: the rule travels with the method. Whatever controller calls it, only ADMIN gets through.
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Book create(BookRequest request) {
        return repository.save(new Book(request.title(), blankToNull(request.author()), request.genre(), request.year()));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Optional<Book> replace(long id, BookRequest request) {
        return repository.findById(id).map(book -> {
            book.update(request.title(), blankToNull(request.author()), request.genre(), request.year());
            return book;
        });
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public boolean delete(long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
