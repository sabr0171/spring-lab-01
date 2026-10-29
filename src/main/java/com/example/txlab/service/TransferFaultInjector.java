package com.example.txlab.service;

import org.springframework.stereotype.Component;

/**
 * Lets tests simulate a crash between the debit and the credit of a transfer (Task A).
 * Disabled by default, so normal transfers are not affected.
 */
@Component
public class TransferFaultInjector {

    private volatile boolean failAfterDebit;

    public void setFailAfterDebit(boolean failAfterDebit) {
        this.failAfterDebit = failAfterDebit;
    }

    public void afterDebit() {
        if (failAfterDebit) {
            throw new SimulatedFailureException("Simulated failure after debit, before credit");
        }
    }
}
