package com.example.txlab.service;

import com.example.txlab.domain.Account;
import com.example.txlab.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Task B: the outer transaction debits an account, writes an audit record through AuditService
 * and then fails after the audit call has returned.
 */
@Service
public class PropagationDemoService {

    private final AccountRepository accountRepository;
    private final AuditService auditService;

    public PropagationDemoService(AccountRepository accountRepository, AuditService auditService) {
        this.accountRepository = accountRepository;
        this.auditService = auditService;
    }

    @Transactional
    public void debitWithRequiredAuditThenFail(Long accountId, BigDecimal amount) {
        debit(accountId, amount);
        auditService.recordInCurrentTransaction("debit " + amount + " from account " + accountId);
        throw new SimulatedFailureException("Outer transaction fails after the REQUIRED audit call returned");
    }

    @Transactional
    public void debitWithRequiresNewAuditThenFail(Long accountId, BigDecimal amount) {
        debit(accountId, amount);
        auditService.recordInNewTransaction("debit " + amount + " from account " + accountId);
        throw new SimulatedFailureException("Outer transaction fails after the REQUIRES_NEW audit call returned");
    }

    private void debit(Long accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId).orElseThrow();
        account.debit(amount);
        accountRepository.flush();
    }
}
