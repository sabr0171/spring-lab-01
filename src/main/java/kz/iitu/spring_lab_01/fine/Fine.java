package kz.iitu.spring_lab_01.fine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import kz.iitu.spring_lab_01.catalog.Book;

// Lab 12, variant 6: a row of fine (V6__fine.sql)
@Entity
@Table(name = "fine")
public class Fine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false, length = 60)
    private String reader;

    @Column(name = "days_overdue", nullable = false)
    private int daysOverdue;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false)
    private boolean paid;

    protected Fine() {
        // for JPA
    }

    public Fine(Book book, String reader, int daysOverdue, int amount) {
        this.book = book;
        this.reader = reader;
        this.daysOverdue = daysOverdue;
        this.amount = amount;
    }

    public void markPaid() {
        this.paid = true;
    }

    public Long getId() {
        return id;
    }

    public Book getBook() {
        return book;
    }

    public String getReader() {
        return reader;
    }

    public int getDaysOverdue() {
        return daysOverdue;
    }

    public int getAmount() {
        return amount;
    }

    public boolean isPaid() {
        return paid;
    }

    @Override
    public String toString() {
        return "Fine[id=" + id + ", reader=" + reader + ", daysOverdue=" + daysOverdue + ", amount=" + amount
                + ", paid=" + paid + "]";
    }
}
