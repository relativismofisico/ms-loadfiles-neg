package com.loadfilesservice.loadfiles.application.exception;

/** Excepción para recursos no encontrados (HTTP 404). */
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Crea una excepción sin mensaje. */
    public ResourceNotFoundException() {
        super();
    }

    /** Crea una excepción con el mensaje indicado. */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /** Crea una excepción con mensaje y causa. */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
