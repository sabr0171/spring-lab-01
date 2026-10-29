package com.example.txlab.service;

import com.example.txlab.domain.Account;
import com.example.txlab.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Task A, checked exceptions: Spring rolls back only on RuntimeException and Error by default.
 * A checked exception thrown from a @Transactional method lets the transaction COMMIT,
 * unless rollbackFor tells Spring otherwise.
 */
@Service
public class CheckedExceptionDemoService {

    private final AccountRepository repository;

    public CheckedExceptionDemoService(AccountRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void debitThenThrowChecked(Long accountId, BigDecimal amount) throws TransferNotificationException {
        debit(accountId, amount);
        throw new TransferNotificationException("Notification service unavailable");
    }

    @Transactional(rollbackFor = TransferNotificationException.class)
    public void debitThenThrowCheckedWithRollbackFor(Long accountId, BigDecimal amount) throws TransferNotificationException {
        debit(accountId, amount);
        throw new TransferNotificationException("Notification service unavailable");
    }

    private void debit(Long accountId, BigDecimal amount) {
        Account account = repository.findById(accountId).orElseThrow();
        account.debit(amount);
    }
}
