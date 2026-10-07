package com.smarthostel.exception;

/** Thrown when a requested record does not exist. Mapped to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String entity, Object id) {
        super(entity + " " + id + " was not found");
    }
}
