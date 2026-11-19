package kz.iitu.spring_lab_01.web;

import kz.iitu.spring_lab_01.catalog.BookNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.List;

// Lab 12: errors of the JSON API as problem details (RFC 9457, Content-Type application/problem+json).
// Limited to @RestController classes, so the Thymeleaf pages keep their usual error pages.
// ResponseEntityExceptionHandler already turns Spring MVC's own exceptions (a body that fails @Valid,
// an unsupported method or media type...) into problem details; this class adds the application's own.
@RestControllerAdvice(annotations = RestController.class)
public class RestErrorAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BookNotFoundException.class)
    ProblemDetail bookNotFound(BookNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("No such book");
        problem.setType(URI.create("https://spring-lab-01.example/problems/book-not-found"));
        problem.setProperty("id", e.getId());
        return problem;
    }

    // A body that fails Bean Validation: the standard 400 problem detail, plus one entry per broken field
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        List<InvalidField> fields = e.getBindingResult().getFieldErrors().stream()
                .map(InvalidField::of)
                .sorted((a, b) -> a.field().compareTo(b.field()))
                .toList();
        ProblemDetail problem = e.getBody();
        problem.setProperty("invalidFields", fields);
        return handleExceptionInternal(e, problem, headers, status, request);
    }

    record InvalidField(String field, String message) {

        static InvalidField of(FieldError error) {
            return new InvalidField(error.getField(), error.getDefaultMessage());
        }
    }
}
