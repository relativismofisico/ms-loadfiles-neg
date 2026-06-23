package com.loadfilesservice.loadfiles.infraestrutura.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuración OpenAPI 3 para la documentación de la API REST. */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ms-loadfiles-neg API")
                        .description("Microservicio de carga y gestión de archivos para el sistema Karonte Factoring. "
                                + "Todos los endpoints requieren autenticación JWT Bearer.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipo Karonte")
                                .email("relativismofisico@gmail.com"))
                        .license(new License()
                                .name("Privada")
                                .url("#")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT obtenido en el microservicio de seguridad. Formato: Bearer <token>")));
    }
}
