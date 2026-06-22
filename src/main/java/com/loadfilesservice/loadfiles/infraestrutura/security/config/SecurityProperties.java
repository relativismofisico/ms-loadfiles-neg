package com.loadfilesservice.loadfiles.infraestrutura.security.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Propiedades de seguridad cargadas desde application.yml. */
@Component
@ConfigurationProperties(prefix = "security")
@Getter
@Setter
public class SecurityProperties {

    private Map<String, List<String>> rol = new HashMap<>();
    private Jwt jwt = new Jwt();

    /** Propiedades del token JWT. */
    @Getter
    @Setter
    public static class Jwt {
        private String secret;
    }
}
