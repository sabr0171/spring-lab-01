package com.example.txlab.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.function.Supplier;

/**
 * Task C: one transaction reads the same data twice; the caller runs another transaction in between.
 * <p>
 * Reads go through JdbcTemplate, not the repository: a second findById in the same transaction
 * would be answered from the persistence context (first-level cache) and never reach the database,
 * so it would hide the database's isolation behaviour.
 */
@Service
public class IsolationDemoService {

    private static final Logger log = LoggerFactory.getLogger(IsolationDemoService.class);

    private final JdbcTemplate jdbc;

    public IsolationDemoService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record TwoReads<T>(T first, T second) {
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TwoReads<BigDecimal> readBalanceTwiceReadCommitted(Long accountId, Runnable betweenReads) {
        return readTwice(() -> balanceOf(accountId), betweenReads);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public TwoReads<BigDecimal> readBalanceTwiceRepeatableRead(Long accountId, Runnable betweenReads) {
        return readTwice(() -> balanceOf(accountId), betweenReads);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TwoReads<Integer> countAccountsTwiceReadCommitted(Runnable betweenReads) {
        return readTwice(this::accountCount, betweenReads);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public TwoReads<Integer> countAccountsTwiceRepeatableRead(Runnable betweenReads) {
        return readTwice(this::accountCount, betweenReads);
    }

    private <T> TwoReads<T> readTwice(Supplier<T> read, Runnable betweenReads) {
        Integer level = jdbc.execute((ConnectionCallback<Integer>) Connection::getTransactionIsolation);
        log.info(">>> JDBC isolation level of this transaction: {}", describe(level));
        T first = read.get();
        log.info(">>> first read: {}", first);
        betweenReads.run();
        T second = read.get();
        log.info(">>> second read: {}", second);
        return new TwoReads<>(first, second);
    }

    private BigDecimal balanceOf(Long accountId) {
        return jdbc.queryForObject("SELECT balance FROM accounts WHERE id = ?", BigDecimal.class, accountId);
    }

    private Integer accountCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM accounts", Integer.class);
    }

    private static String describe(Integer level) {
        if (level == null) return "unknown";
        return switch (level) {
            case Connection.TRANSACTION_READ_UNCOMMITTED -> "READ_UNCOMMITTED";
            case Connection.TRANSACTION_READ_COMMITTED -> "READ_COMMITTED";
            case Connection.TRANSACTION_REPEATABLE_READ -> "REPEATABLE_READ";
            case Connection.TRANSACTION_SERIALIZABLE -> "SERIALIZABLE";
            default -> String.valueOf(level);
        };
    }
}
