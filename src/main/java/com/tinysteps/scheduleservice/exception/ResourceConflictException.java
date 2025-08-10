package com.tinysteps.scheduleservice.exception;

/**
 * Thrown when there is a conflict with the current state of the resource.
 * Example: Trying to create an appointment in a slot that's already taken.
 */
public class ResourceConflictException extends RuntimeException {

    public ResourceConflictException(String message) {
        super(message);
    }
}
