package com.example.txlab.service;

import com.example.txlab.domain.AuditRecord;
import com.example.txlab.repository.AuditRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Task B: the inner service of the propagation experiment.
 * It is a separate bean on purpose: a call from another bean goes through the Spring proxy,
 * so the propagation setting is applied. A call to this.method() inside one bean would bypass it.
 */
@Service
public class AuditService {

    private final AuditRecordRepository repository;

    public AuditService(AuditRecordRepository repository) {
        this.repository = repository;
    }

    /** Joins the caller's transaction: commits or rolls back together with it. */
    @Transactional(propagation = Propagation.REQUIRED)
    public void recordInCurrentTransaction(String message) {
        repository.save(new AuditRecord("[REQUIRED] " + message));
    }

    /** Suspends the caller's transaction and commits in its own one, before the caller continues. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordInNewTransaction(String message) {
        repository.save(new AuditRecord("[REQUIRES_NEW] " + message));
    }

    /** Refuses to run without an existing transaction. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordMandatory(String message) {
        repository.save(new AuditRecord("[MANDATORY] " + message));
    }
}
