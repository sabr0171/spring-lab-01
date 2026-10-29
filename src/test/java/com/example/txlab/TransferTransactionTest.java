package com.example.txlab;

import com.example.txlab.service.CheckedExceptionDemoService;
import com.example.txlab.service.SimulatedFailureException;
import com.example.txlab.service.TransferFaultInjector;
import com.example.txlab.service.TransferNotificationException;
import com.example.txlab.service.TransferService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Task A: declarative transactions and rollback. */
class TransferTransactionTest extends AbstractLabTest {

    @Autowired
    TransferService transferService;

    @Autowired
    TransferFaultInjector faultInjector;

    @Autowired
    CheckedExceptionDemoService checkedExceptionDemo;

    @AfterEach
    void disableFault() {
        faultInjector.setFailAfterDebit(false);
    }

    @Test
    void successfulTransferMovesMoney() {
        logBalances("Before transfer of 100.00");

        transferService.transfer(aliceId, bobId, new BigDecimal("100.00"));

        logBalances("After transfer of 100.00");
        assertThat(balanceOf(aliceId)).isEqualByComparingTo("900.00");
        assertThat(balanceOf(bobId)).isEqualByComparingTo("600.00");
    }

    @Test
    void insufficientFundsLeavesBothBalancesUnchanged() {
        assertThatThrownBy(() -> transferService.transfer(aliceId, bobId, new BigDecimal("5000.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Insufficient funds");

        logBalances("After failed transfer of 5000.00");
        assertThat(balanceOf(aliceId)).isEqualByComparingTo("1000.00");
        assertThat(balanceOf(bobId)).isEqualByComparingTo("500.00");
    }

    @Test
    void uncheckedExceptionAfterDebitRollsBackTheDebit() {
        faultInjector.setFailAfterDebit(true);

        assertThatThrownBy(() -> transferService.transfer(aliceId, bobId, new BigDecimal("100.00")))
                .isInstanceOf(SimulatedFailureException.class);

        logBalances("After unchecked exception between debit and credit");
        assertThat(balanceOf(aliceId)).isEqualByComparingTo("1000.00");
        assertThat(balanceOf(bobId)).isEqualByComparingTo("500.00");
        // The debit UPDATE was flushed (version 0 -> 1) before the failure; the rollback undid it too
        assertThat(versionOf(aliceId)).isZero();
    }

    @Test
    void checkedExceptionCommitsByDefault() {
        assertThatThrownBy(() -> checkedExceptionDemo.debitThenThrowChecked(aliceId, new BigDecimal("100.00")))
                .isInstanceOf(TransferNotificationException.class);

        logBalances("After checked exception, default rollback rules");
        // The caller got an exception, yet the debit was committed: 100.00 left Alice and arrived nowhere
        assertThat(balanceOf(aliceId)).isEqualByComparingTo("900.00");
        assertThat(balanceOf(bobId)).isEqualByComparingTo("500.00");
    }

    @Test
    void checkedExceptionRollsBackWithRollbackFor() {
        assertThatThrownBy(() -> checkedExceptionDemo.debitThenThrowCheckedWithRollbackFor(aliceId, new BigDecimal("100.00")))
                .isInstanceOf(TransferNotificationException.class);

        logBalances("After checked exception with rollbackFor");
        assertThat(balanceOf(aliceId)).isEqualByComparingTo("1000.00");
    }
}
