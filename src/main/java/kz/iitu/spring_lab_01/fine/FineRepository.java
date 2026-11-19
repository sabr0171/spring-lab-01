package kz.iitu.spring_lab_01.fine;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FineRepository extends JpaRepository<Fine, Long> {

    // What a reader still owes, the biggest fine first. The book is fetched with the fine:
    // the JSON is written after the service's transaction has ended (open-in-view is off).
    @EntityGraph(attributePaths = "book")
    List<Fine> findByReaderAndPaidFalseOrderByAmountDescIdAsc(String reader);

    @Query("select coalesce(sum(f.amount), 0) from Fine f where f.reader = :reader and f.paid = false")
    long totalUnpaid(@Param("reader") String reader);
}
