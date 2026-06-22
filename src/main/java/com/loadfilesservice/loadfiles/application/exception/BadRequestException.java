package com.loadfilesservice.loadfiles.application.exception;

/** Excepción para solicitudes inválidas (HTTP 400). */
public class BadRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Crea una excepción sin mensaje. */
    public BadRequestException() {
        super();
    }

    /** Crea una excepción con el mensaje indicado. */
    public BadRequestException(String message) {
        super(message);
    }

    /** Crea una excepción con mensaje y causa. */
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

}
