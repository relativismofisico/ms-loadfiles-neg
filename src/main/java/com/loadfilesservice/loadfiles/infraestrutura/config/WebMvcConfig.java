package com.loadfilesservice.loadfiles.infraestrutura.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Configuración MVC: agrega el prefijo /load a todos los controladores REST. */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/load",
            c -> c.isAnnotationPresent(RestController.class)
                && c.getPackageName().startsWith("com.loadfilesservice"));
    }
}
