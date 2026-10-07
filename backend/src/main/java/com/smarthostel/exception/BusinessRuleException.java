package com.smarthostel.exception;

/** Thrown when a request breaks a hostel business rule. Mapped to HTTP 400. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
