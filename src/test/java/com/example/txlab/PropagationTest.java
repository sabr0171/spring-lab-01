package com.example.txlab;

import com.example.txlab.service.AuditService;
import com.example.txlab.service.PropagationDemoService;
import com.example.txlab.service.SimulatedFailureException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Task B: transaction propagation. */
class PropagationTest extends AbstractLabTest {

    @Autowired
    PropagationDemoService propagationDemo;

    @Autowired
    AuditService auditService;

    @Test
    void requiredAuditIsRolledBackWithOuterTransaction() {
        assertThatThrownBy(() -> propagationDemo.debitWithRequiredAuditThenFail(aliceId, new BigDecimal("100.00")))
                .isInstanceOf(SimulatedFailureException.class);

        log.info(">>> REQUIRED: audit records after outer rollback = {}", auditCount());
        logBalances("REQUIRED, after outer rollback");
        assertThat(auditCount()).isZero();
        assertThat(balanceOf(aliceId)).isEqualByComparingTo("1000.00");
    }

    @Test
    void requiresNewAuditSurvivesOuterRollback() {
        assertThatThrownBy(() -> propagationDemo.debitWithRequiresNewAuditThenFail(aliceId, new BigDecimal("100.00")))
                .isInstanceOf(SimulatedFailureException.class);

        log.info(">>> REQUIRES_NEW: audit records after outer rollback = {}", auditCount());
        logBalances("REQUIRES_NEW, after outer rollback");
        assertThat(auditCount()).isEqualTo(1);
        assertThat(balanceOf(aliceId)).isEqualByComparingTo("1000.00");
    }

    @Test
    void mandatoryFailsWithoutExistingTransaction() {
        assertThatThrownBy(() -> auditService.recordMandatory("called without a transaction"))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThat(auditCount()).isZero();
    }
}
