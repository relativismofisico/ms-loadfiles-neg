package com.loadfilesservice.loadfiles.application.exception;

public class BadRequestException extends RuntimeException {

    public BadRequestException() {
        super();
    }

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    private static final long serialVersionUID = 1L;

}
