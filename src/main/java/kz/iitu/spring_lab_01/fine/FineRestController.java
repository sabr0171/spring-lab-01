package kz.iitu.spring_lab_01.fine;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

// Lab 12, variant 6. The librarian (ADMIN) fines a reader for a book; every reader sees only their own fines.
@RestController
public class FineRestController {

    private final FineService service;

    public FineRestController(FineService service) {
        this.service = service;
    }

    @PostMapping("/api/books/{bookId}/fines")
    public ResponseEntity<FineView> charge(@PathVariable long bookId, @Valid @RequestBody FineRequest request) {
        Fine fine = service.charge(bookId, request.reader().strip(), request.daysOverdue());
        return ResponseEntity.created(URI.create("/api/fines/" + fine.getId())).body(FineView.of(fine));
    }

    @GetMapping("/api/fines/mine")
    public Statement mine(Authentication authentication) {
        String reader = authentication.getName();
        return new Statement(reader, service.totalOwedBy(reader),
                service.unpaidOf(reader).stream().map(FineView::of).toList());
    }

    public record FineRequest(
            @NotBlank @Size(max = 60) String reader,
            @Min(1) @Max(3650) int daysOverdue) {
    }

    public record FineView(Long id, Long bookId, String bookTitle, String reader, int daysOverdue, int amount) {

        static FineView of(Fine fine) {
            return new FineView(fine.getId(), fine.getBook().getId(), fine.getBook().getTitle(), fine.getReader(),
                    fine.getDaysOverdue(), fine.getAmount());
        }
    }

    public record Statement(String reader, long total, List<FineView> fines) {
    }
}
