package com.loadfilesservice.loadfiles.application.exception;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/** Respuesta estándar de error de la API. */
@Schema(description = "Respuesta estándar de error de la API REST")
public record ApiErrorResponse(
        @Schema(description = "Fecha y hora en que ocurrió el error", example = "2024-01-01T12:00:00")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,
        @Schema(description = "Código HTTP del error", example = "404")
        int status,
        @Schema(description = "Descripción del estado HTTP", example = "Not Found")
        String error,
        @Schema(description = "Mensaje descriptivo del error", example = "No se encontró el recurso solicitado")
        String message,
        @Schema(description = "Ruta de la solicitud que generó el error", example = "/load/api/companyfile/upload")
        String path
) {
    /** Construye una respuesta de error con la fecha y hora actual. */
    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(LocalDateTime.now(), status, error, message, path);
    }
}
