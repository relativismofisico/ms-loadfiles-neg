package com.loadfilesservice.loadfiles.infraestrutura.security.auth;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import com.loadfilesservice.loadfiles.infraestrutura.security.config.SecurityProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component("rolValidator")
@RequiredArgsConstructor
@Slf4j
public class RolValidator {

    private final SecurityProperties securityProperties;

    public boolean hasRol(Authentication authentication, String grupo) {
        List<String> rolesPermitidos = securityProperties.getRol().get(grupo);
        if (rolesPermitidos == null || rolesPermitidos.isEmpty()) {
            log.warn("[RolValidator] Grupo de roles no encontrado en configuración: {}", grupo);
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(rolesPermitidos::contains);
    }
}
