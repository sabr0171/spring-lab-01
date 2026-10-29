package com.example.txlab.service;

/** Checked exception: by default Spring does NOT roll back a transaction because of it. */
public class TransferNotificationException extends Exception {

    public TransferNotificationException(String message) {
        super(message);
    }
}
