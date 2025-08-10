package com.tinysteps.scheduleservice.exception;

/**
 * Thrown when a requested resource (entity) is not found in the system.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
