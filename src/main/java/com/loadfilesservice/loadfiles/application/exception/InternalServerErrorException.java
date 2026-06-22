package com.loadfilesservice.loadfiles.application.exception;

/** Excepción para errores internos del servidor (HTTP 500). */
public class InternalServerErrorException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Crea una excepción sin mensaje. */
    public InternalServerErrorException() {
        super();
    }

    /** Crea una excepción con el mensaje indicado. */
    public InternalServerErrorException(String message) {
        super(message);
    }

    /** Crea una excepción con mensaje y causa. */
    public InternalServerErrorException(String message, Throwable cause) {
        super(message, cause);
    }

}
