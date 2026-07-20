package com.cms.exception;

/**
 * Root of the custom exception hierarchy for the Complaint Management System.
 * Every domain-specific exception extends this so calling code can choose to
 * catch broadly (BaseException) or narrowly (a specific subtype).
 */
public class BaseException extends Exception {

    public BaseException(String message) {
        super(message);
    }

    public BaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
