package kz.iitu.spring_lab_01.catalog;

// Lab 12: a book id that does not exist. RestErrorAdvice answers it with 404 and a problem detail.
public class BookNotFoundException extends RuntimeException {

    private final long id;

    public BookNotFoundException(long id) {
        super("Book " + id + " does not exist");
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
