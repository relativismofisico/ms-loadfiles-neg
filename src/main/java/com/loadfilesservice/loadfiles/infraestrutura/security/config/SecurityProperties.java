package com.loadfilesservice.loadfiles.infraestrutura.security.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "security")
@Getter
@Setter
public class SecurityProperties {

    private Map<String, List<String>> rol = new HashMap<>();
    private Jwt jwt = new Jwt();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
    }
}
