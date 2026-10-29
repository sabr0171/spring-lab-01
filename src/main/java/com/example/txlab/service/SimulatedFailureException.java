package com.example.txlab.service;

/** Unchecked exception used to break a transaction on purpose in the experiments. */
public class SimulatedFailureException extends RuntimeException {

    public SimulatedFailureException(String message) {
        super(message);
    }
}
