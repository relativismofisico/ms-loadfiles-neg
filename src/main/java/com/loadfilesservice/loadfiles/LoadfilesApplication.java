package com.loadfilesservice.loadfiles;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Punto de entrada de la aplicación ms-loadfiles-neg. */
@SpringBootApplication
public final class LoadfilesApplication {

    private LoadfilesApplication() {
    }

    /** Inicia la aplicación Spring Boot. */
    public static void main(String[] args) {
        SpringApplication.run(LoadfilesApplication.class, args);
    }

}
