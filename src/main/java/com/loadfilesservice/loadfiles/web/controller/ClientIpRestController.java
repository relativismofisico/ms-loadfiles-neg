package com.loadfilesservice.loadfiles.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone la IP pública del cliente que hace la petición, para que los frontends dejen de depender
 * de un servicio de terceros (p.ej. api.ipify.org) para capturarla con fines de auditoría.
 */
@Tag(name = "IP del cliente", description = "Captura de la IP del cliente sin depender de servicios externos")
@CrossOrigin(origins = {"http://localhost:4300", "http://localhost:4400", "http://localhost:4500",
        "http://localhost:4600", "http://localhost:4700"})
@RestController
@RequestMapping("/api/ip")
public class ClientIpRestController {

    /** Retorna la IP del cliente, tomando el primer valor de X-Forwarded-For si viene detrás de un proxy. */
    @Operation(summary = "Obtener la IP del cliente",
        description = "Endpoint público. Resuelve X-Forwarded-For (proxy/gateway) y cae a la IP de la conexión.")
    @GetMapping(produces = "application/json")
    public ResponseEntity<Map<String, String>> getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String ip;
        if (StringUtils.hasText(forwardedFor)) {
            ip = forwardedFor.split(",")[0].trim();
        } else {
            ip = request.getRemoteAddr();
        }

        return ResponseEntity.ok(Map.of("ip", ip));
    }

}
