package com.loadfilesservice.loadfiles.security.auth;

import com.loadfilesservice.loadfiles.infraestrutura.security.auth.RolValidator;
import com.loadfilesservice.loadfiles.infraestrutura.security.config.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolValidatorTest {

    @Mock
    private SecurityProperties securityProperties;

    @InjectMocks
    private RolValidator rolValidator;

    private Map<String, List<String>> rolConfig;

    @BeforeEach
    void setUp() {
        rolConfig = Map.of(
                "todos", List.of("ADMINISTRADOR", "EMPRESA", "OPERARIO", "FONDEADOR"),
                "carga", List.of("ADMINISTRADOR", "EMPRESA", "OPERARIO"),
                "revision", List.of("ADMINISTRADOR", "OPERARIO", "FONDEADOR"),
                "firma-subida", List.of("ADMINISTRADOR", "OPERARIO"),
                "firma-guardado", List.of("ADMINISTRADOR", "EMPRESA")
        );
        when(securityProperties.getRol()).thenReturn(rolConfig);
    }

    private Authentication authWith(String rol) {
        return new UsernamePasswordAuthenticationToken(
                "user@test.com", null, List.of(new SimpleGrantedAuthority(rol)));
    }

    @Test
    void hasRol_shouldReturnTrue_whenRolIsInGroup() {
        assertThat(rolValidator.hasRol(authWith("ADMINISTRADOR"), "todos")).isTrue();
        assertThat(rolValidator.hasRol(authWith("EMPRESA"), "carga")).isTrue();
        assertThat(rolValidator.hasRol(authWith("FONDEADOR"), "revision")).isTrue();
        assertThat(rolValidator.hasRol(authWith("OPERARIO"), "firma-subida")).isTrue();
        assertThat(rolValidator.hasRol(authWith("EMPRESA"), "firma-guardado")).isTrue();
    }

    @Test
    void hasRol_shouldReturnFalse_whenRolIsNotInGroup() {
        assertThat(rolValidator.hasRol(authWith("FONDEADOR"), "carga")).isFalse();
        assertThat(rolValidator.hasRol(authWith("EMPRESA"), "revision")).isFalse();
        assertThat(rolValidator.hasRol(authWith("FONDEADOR"), "firma-subida")).isFalse();
        assertThat(rolValidator.hasRol(authWith("OPERARIO"), "firma-guardado")).isFalse();
    }

    @Test
    void hasRol_shouldReturnFalse_whenGroupDoesNotExist() {
        assertThat(rolValidator.hasRol(authWith("ADMINISTRADOR"), "grupo-inexistente")).isFalse();
    }

    @Test
    void hasRol_shouldReturnTrue_forAllRoles_inTodosGroup() {
        for (String rol : List.of("ADMINISTRADOR", "EMPRESA", "OPERARIO", "FONDEADOR")) {
            assertThat(rolValidator.hasRol(authWith(rol), "todos"))
                    .as("El rol %s debe tener acceso al grupo 'todos'", rol)
                    .isTrue();
        }
    }
}