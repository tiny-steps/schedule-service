package com.tinysteps.scheduleservice.exception;

/**
 * Thrown when an integration with another microservice fails or returns an error.
 */
public class IntegrationException extends RuntimeException {

    public IntegrationException(String message) {
        super(message);
    }

    public IntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
