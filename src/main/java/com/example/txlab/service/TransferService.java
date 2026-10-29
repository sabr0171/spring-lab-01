package com.example.txlab.service;

import com.example.txlab.domain.Account;
import com.example.txlab.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Task A: a money transfer is one atomic unit of work.
 * Either both the debit and the credit are committed, or neither is.
 */
@Service
public class TransferService {

    private final AccountRepository repository;
    private final TransferFaultInjector faultInjector;

    public TransferService(AccountRepository repository, TransferFaultInjector faultInjector) {
        this.repository = repository;
        this.faultInjector = faultInjector;
    }

    @Transactional
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        if (fromId.equals(toId)) throw new IllegalArgumentException("Cannot transfer to the same account");

        Account from = repository.findById(fromId).orElseThrow();
        Account to = repository.findById(toId).orElseThrow();

        from.debit(amount);
        // Flush so the debit UPDATE really reaches the database before a possible failure:
        // the rollback test then proves the database undid it, not just that Hibernate never sent it.
        repository.saveAndFlush(from);

        faultInjector.afterDebit();

        to.credit(amount);
        repository.save(to);
    }
}
