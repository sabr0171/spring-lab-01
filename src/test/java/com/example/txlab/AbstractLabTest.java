package com.example.txlab;

import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

/**
 * Tests are deliberately NOT @Transactional: the experiments need real commits and rollbacks,
 * not one test transaction wrapped around everything. The data is reset before each test instead.
 */
@SpringBootTest
public abstract class AbstractLabTest {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    protected JdbcTemplate jdbc;

    protected Long aliceId;
    protected Long bobId;

    @BeforeEach
    void resetData() {
        jdbc.update("DELETE FROM audit_log");
        jdbc.update("DELETE FROM accounts WHERE owner_name NOT IN ('Alice', 'Bob')");
        jdbc.update("UPDATE accounts SET balance = 1000.00, version = 0 WHERE owner_name = 'Alice'");
        jdbc.update("UPDATE accounts SET balance = 500.00, version = 0 WHERE owner_name = 'Bob'");
        aliceId = jdbc.queryForObject("SELECT id FROM accounts WHERE owner_name = 'Alice'", Long.class);
        bobId = jdbc.queryForObject("SELECT id FROM accounts WHERE owner_name = 'Bob'", Long.class);
    }

    protected BigDecimal balanceOf(Long accountId) {
        return jdbc.queryForObject("SELECT balance FROM accounts WHERE id = ?", BigDecimal.class, accountId);
    }

    protected long versionOf(Long accountId) {
        return jdbc.queryForObject("SELECT version FROM accounts WHERE id = ?", Long.class, accountId);
    }

    protected int auditCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM audit_log", Integer.class);
    }

    protected void logBalances(String when) {
        log.info(">>> {}: Alice={} (version {}), Bob={} (version {})", when,
                balanceOf(aliceId), versionOf(aliceId), balanceOf(bobId), versionOf(bobId));
    }
}
