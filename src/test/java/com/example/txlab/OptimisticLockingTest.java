package com.example.txlab;

import com.example.txlab.domain.Account;
import com.example.txlab.repository.AccountRepository;
import com.example.txlab.service.OptimisticLockingDemoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Task D: optimistic locking with @Version. */
class OptimisticLockingTest extends AbstractLabTest {

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    OptimisticLockingDemoService optimisticDemo;

    @Test
    void staleCopyCannotOverwriteNewerUpdate() {
        // Two independent transactions (and persistence contexts) load the same row: both copies have version 0
        Account first = transactionTemplate.execute(status -> accountRepository.findById(aliceId).orElseThrow());
        Account second = transactionTemplate.execute(status -> accountRepository.findById(aliceId).orElseThrow());
        log.info(">>> loaded two copies: {} and {}", first, second);

        first.debit(new BigDecimal("100.00"));
        transactionTemplate.executeWithoutResult(status -> accountRepository.save(first));
        logBalances("First copy committed");

        second.debit(new BigDecimal("300.00"));
        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> accountRepository.save(second)))
                .isInstanceOf(OptimisticLockingFailureException.class)
                .satisfies(ex -> log.info(">>> stale copy rejected: {}", ex.toString()));

        logBalances("After stale copy was rejected");
        assertThat(balanceOf(aliceId)).isEqualByComparingTo("900.00");
        assertThat(versionOf(aliceId)).isEqualTo(1);
    }

    @Test
    void secondOfTwoConcurrentTransactionsFailsOnCommit() throws Exception {
        CountDownLatch bothLoaded = new CountDownLatch(1);
        CountDownLatch firstCommitted = new CountDownLatch(1);
        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            // Thread A waits until B has read the row too, so both start from version 0
            Future<?> a = threads.submit(() ->
                    optimisticDemo.debitAfterPause(aliceId, new BigDecimal("100.00"), () -> await(bothLoaded)));
            // Thread B reads, then holds its stale copy until A has committed
            Future<?> b = threads.submit(() ->
                    optimisticDemo.debitAfterPause(aliceId, new BigDecimal("300.00"), () -> {
                        bothLoaded.countDown();
                        await(firstCommitted);
                    }));

            a.get(10, TimeUnit.SECONDS);
            logBalances("Transaction A committed");
            firstCommitted.countDown();

            assertThatThrownBy(() -> b.get(10, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(OptimisticLockingFailureException.class)
                    .satisfies(ex -> log.info(">>> transaction B rejected: {}", ex.getCause().toString()));
        } finally {
            threads.shutdownNow();
        }

        logBalances("After transaction B was rejected");
        assertThat(balanceOf(aliceId)).isEqualByComparingTo("900.00");
        assertThat(versionOf(aliceId)).isEqualTo(1);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out waiting for the other transaction");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
