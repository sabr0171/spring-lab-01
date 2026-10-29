package com.example.txlab.web;

import com.example.txlab.domain.Account;
import com.example.txlab.service.AccountService;
import com.example.txlab.service.TransferService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AccountController {

    private final AccountService accountService;
    private final TransferService transferService;

    public AccountController(AccountService accountService, TransferService transferService) {
        this.accountService = accountService;
        this.transferService = transferService;
    }

    public record TransferRequest(Long fromId, Long toId, BigDecimal amount) {
    }

    @GetMapping("/accounts")
    public List<Account> accounts() {
        return accountService.findAll();
    }

    @PostMapping("/transfers")
    public ResponseEntity<Void> transfer(@RequestBody TransferRequest request) {
        transferService.transfer(request.fromId(), request.toId(), request.amount());
        return ResponseEntity.noContent().build();
    }
}
