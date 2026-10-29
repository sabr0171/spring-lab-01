package com.example.txlab;

import com.example.txlab.service.AccountService;
import com.example.txlab.service.IsolationDemoService;
import com.example.txlab.service.IsolationDemoService.TwoReads;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Task C: isolation levels, observed on H2 (in-memory, MVStore engine).
 * Transaction 1 runs in the test thread and reads twice; transaction 2 runs in another thread,
 * with its own connection, and commits between the two reads.
 */
class IsolationTest extends AbstractLabTest {

    @Autowired
    IsolationDemoService isolationDemo;

    @Autowired
    AccountService accountService;

    private final ExecutorService otherThread = Executors.newSingleThreadExecutor();

    @AfterEach
    void stopOtherThread() {
        otherThread.shutdownNow();
    }

    /** Runs work as a separate transaction in another thread and waits until it has committed. */
    private Runnable committedInOtherThread(Runnable work) {
        return () -> {
            try {
                otherThread.submit(work).get(10, TimeUnit.SECONDS);
                log.info(">>> concurrent transaction committed");
            } catch (Exception e) {
                throw new IllegalStateException("Concurrent transaction failed", e);
            }
        };
    }

    private Runnable depositToAlice() {
        return committedInOtherThread(() -> accountService.deposit(aliceId, new BigDecimal("50.00")));
    }

    private Runnable openNewAccount() {
        return committedInOtherThread(() -> accountService.open("Charlie", new BigDecimal("10.00")));
    }

    @Test
    void readCommittedSeesCommittedUpdateOnSecondRead() {
        TwoReads<BigDecimal> reads = isolationDemo.readBalanceTwiceReadCommitted(aliceId, depositToAlice());

        // Non-repeatable read: the same row returned a different value inside one transaction
        assertThat(reads.first()).isEqualByComparingTo("1000.00");
        assertThat(reads.second()).isEqualByComparingTo("1050.00");
    }

    @Test
    void repeatableReadKeepsFirstValueOnSecondRead() {
        TwoReads<BigDecimal> reads = isolationDemo.readBalanceTwiceRepeatableRead(aliceId, depositToAlice());

        assertThat(reads.first()).isEqualByComparingTo("1000.00");
        assertThat(reads.second()).isEqualByComparingTo("1000.00");
        // The other transaction's change is committed; transaction 1 just did not see it
        assertThat(balanceOf(aliceId)).isEqualByComparingTo("1050.00");
    }

    @Test
    void readCommittedSeesPhantomRow() {
        TwoReads<Integer> counts = isolationDemo.countAccountsTwiceReadCommitted(openNewAccount());

        assertThat(counts.first()).isEqualTo(2);
        assertThat(counts.second()).isEqualTo(3);
    }

    @Test
    void repeatableReadOnH2AlsoHidesNewRows() {
        TwoReads<Integer> counts = isolationDemo.countAccountsTwiceRepeatableRead(openNewAccount());

        // The SQL standard allows phantoms at REPEATABLE READ, but H2 (MVStore) reads from a snapshot,
        // so the row inserted by the other transaction is not visible either
        assertThat(counts.first()).isEqualTo(2);
        assertThat(counts.second()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM accounts", Integer.class)).isEqualTo(3);
    }
}
