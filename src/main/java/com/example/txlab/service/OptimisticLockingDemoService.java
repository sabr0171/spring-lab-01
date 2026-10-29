package com.example.txlab.service;

import com.example.txlab.domain.Account;
import com.example.txlab.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Task D: loads an account, pauses so the caller can line up another transaction on the same row,
 * then debits it. The UPDATE runs at commit as "... WHERE id = ? AND version = ?".
 */
@Service
public class OptimisticLockingDemoService {

    private final AccountRepository repository;

    public OptimisticLockingDemoService(AccountRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void debitAfterPause(Long accountId, BigDecimal amount, Runnable afterLoad) {
        Account account = repository.findById(accountId).orElseThrow();
        afterLoad.run();
        account.debit(amount);
    }
}
