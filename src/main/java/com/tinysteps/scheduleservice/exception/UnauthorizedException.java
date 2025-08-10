package com.tinysteps.scheduleservice.exception;

/**
 * Thrown when the current user is not authorized to perform the requested action.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
