package kz.iitu.spring_lab_01.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByAuthorContainingIgnoreCaseOrderByIdAsc(String author);
}
