package com.loadfilesservice.loadfiles.security.auth;

import com.loadfilesservice.loadfiles.security.config.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("rolValidator")
@RequiredArgsConstructor
@Slf4j
public class RolValidator {

    private final SecurityProperties securityProperties;

    /**
     * Verifica si la autenticación actual posee algún rol del grupo indicado.
     * Los grupos y sus roles se configuran en application.yml bajo security.rol.
     */
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
