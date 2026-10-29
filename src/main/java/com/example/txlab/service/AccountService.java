package com.example.txlab.service;

import com.example.txlab.domain.Account;
import com.example.txlab.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository repository;

    public AccountService(AccountRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Account> findAll() {
        return repository.findAll();
    }

    @Transactional
    public Account open(String ownerName, BigDecimal initialBalance) {
        return repository.save(new Account(ownerName, initialBalance));
    }

    @Transactional
    public Account deposit(Long accountId, BigDecimal amount) {
        Account account = repository.findById(accountId).orElseThrow();
        account.credit(amount);
        return account;
    }
}
