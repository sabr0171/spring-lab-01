package kz.iitu.spring_lab_01.web;

import jakarta.validation.Valid;
import kz.iitu.spring_lab_01.catalog.Book;
import kz.iitu.spring_lab_01.catalog.BookRequest;
import kz.iitu.spring_lab_01.catalog.BookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

// Lab 10: no URL rule opens /api/**, so the last rule of the filter chain (denyAll) closes this controller.
// BookService still checks the role itself (@PreAuthorize), in case /api/** is ever opened.
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
    public ResponseEntity<String> citation(@PathVariable long id) {
        return service.findById(id)
                .map(b -> ResponseEntity.ok(
                        b.getAuthor() + ". " + b.getTitle() + ". " + b.getYear() + "."))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Book> create(@Valid @RequestBody BookRequest request) {
        Book saved = service.create(request);
        return ResponseEntity
                .created(URI.create("/api/books/" + saved.getId()))
                .body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Book> replace(@PathVariable long id, @Valid @RequestBody BookRequest request) {
        return service.replace(id, request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        return service.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Book> get(@PathVariable long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
