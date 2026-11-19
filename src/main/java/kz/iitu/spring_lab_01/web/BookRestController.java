package kz.iitu.spring_lab_01.web;

import jakarta.validation.Valid;
import kz.iitu.spring_lab_01.catalog.Book;
import kz.iitu.spring_lab_01.catalog.BookNotFoundException;
import kz.iitu.spring_lab_01.catalog.BookRequest;
import kz.iitu.spring_lab_01.catalog.BookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

// Lab 12: /api/** has its own stateless filter chain with HTTP Basic (SecurityConfiguration.apiFilterChain):
// reading for USER and ADMIN, writing for ADMIN. BookService still checks the role itself (@PreAuthorize).
// A missing book is a BookNotFoundException, answered by RestErrorAdvice with 404 and a problem detail.
@RestController
@RequestMapping("/api/books")
public class BookRestController {

    private final BookService service;

    public BookRestController(BookService service) {
        this.service = service;
    }

    @GetMapping
    public List<Book> list(@RequestParam(required = false) String author,
                           @RequestParam(defaultValue = "10") int limit) {
        return service.findAll(author).stream().limit(limit).toList();
    }

    @GetMapping(value = "/{id}/citation", produces = "text/plain")
    public String citation(@PathVariable long id) {
        Book b = service.requireById(id);
        return b.getAuthor() + ". " + b.getTitle() + ". " + b.getYear() + ".";
    }

    @PostMapping
    public ResponseEntity<Book> create(@Valid @RequestBody BookRequest request) {
        Book saved = service.create(request);
        return ResponseEntity
                .created(URI.create("/api/books/" + saved.getId()))
                .body(saved);
    }

    @PutMapping("/{id}")
    public Book replace(@PathVariable long id, @Valid @RequestBody BookRequest request) {
        return service.replace(id, request).orElseThrow(() -> new BookNotFoundException(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        if (!service.delete(id)) {
            throw new BookNotFoundException(id);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public Book get(@PathVariable long id) {
        return service.requireById(id);
    }
}
