package com.tinysteps.scheduleservice.exception;

/**
 * Thrown when the request data is invalid or violates business rules.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
