package kz.iitu.spring_lab_01.web;

import kz.iitu.spring_lab_01.catalog.Book;
import kz.iitu.spring_lab_01.catalog.BookNotFoundException;
import kz.iitu.spring_lab_01.catalog.BookService;
import kz.iitu.spring_lab_01.catalog.Genre;
import kz.iitu.spring_lab_01.security.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Task 2. A slice: BookRestController with what surrounds it on the web side — RestErrorAdvice, Jackson,
// Bean Validation, the security filters. No service, no repository, no database: BookService is a @MockitoBean.
//
// Security: @WebMvcTest does not load @Configuration classes, so on its own it would run under Spring Boot's
// default chain, not ours. SecurityConfiguration is imported — the real /api/** rules and the CSP of lab 10
// apply — and @WithMockUser supplies a user with the role a rule asks for. Security is not switched off.
@WebMvcTest(BookRestController.class)
@Import(SecurityConfiguration.class)
class BookControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    BookService books;

    // 1. 200 and the fields of the JSON contract (and the API answer carries the Content-Security-Policy too)
    @Test
    @WithMockUser(roles = "USER")
    void aReaderGetsAnExistingBookAsJson() throws Exception {
        Book book = new Book("Clean Code", "Robert Martin", Genre.PROGRAMMING, 2008);
        ReflectionTestUtils.setField(book, "id", 2L);
        when(books.requireById(2L)).thenReturn(book);

        mvc.perform(get("/api/books/2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andExpect(jsonPath("$.author").value("Robert Martin"))
                .andExpect(jsonPath("$.genre").value("PROGRAMMING"))
                .andExpect(jsonPath("$.year").value(2008))
                .andExpect(header().string("Content-Security-Policy", startsWith("default-src 'self'")));
    }

    // 2. 404 with the problem detail of RestErrorAdvice
    @Test
    @WithMockUser(roles = "USER")
    void aMissingBookIsA404ProblemDetail() throws Exception {
        when(books.requireById(99L)).thenThrow(new BookNotFoundException(99L));

        mvc.perform(get("/api/books/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("No such book"))
                .andExpect(jsonPath("$.detail").value("Book 99 does not exist"))
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.instance").value("/api/books/99"));
    }

    // 3. 400: the body breaks three constraints of BookRequest, and the service is never reached
    @Test
    @WithMockUser(roles = "ADMIN")
    void aBookThatBreaksTheValidationRulesIsA400() throws Exception {
        mvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "", "author": "Nobody", "year": 3000}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.invalidFields[*].field").value(containsInAnyOrder("genre", "title", "year")));
        verifyNoInteractions(books);
    }

    // The chain is really there: nobody logged in -> 401 with a Basic challenge ...
    @Test
    void withoutCredentialsTheAnswerIs401() throws Exception {
        mvc.perform(get("/api/books/2"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", startsWith("Basic")));
    }

    // ... and a reader may not delete: refused by the URL rule before the controller runs
    @Test
    @WithMockUser(roles = "USER")
    void aReaderMayNotDeleteABook() throws Exception {
        mvc.perform(delete("/api/books/2"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(books);
    }
}
