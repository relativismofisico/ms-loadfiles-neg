package com.loadfilesservice.loadfiles.security.auth

import com.loadfilesservice.loadfiles.infraestrutura.security.auth.RolValidator
import com.loadfilesservice.loadfiles.infraestrutura.security.config.SecurityProperties
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import spock.lang.Specification
import spock.lang.Unroll

class RolValidatorSpec extends Specification {

    SecurityProperties securityProperties = Mock()
    RolValidator rolValidator = new RolValidator(securityProperties)

    def rolConfig = [
        "todos"        : ["ADMINISTRADOR", "EMPRESA", "OPERARIO", "FONDEADOR"],
        "carga"        : ["ADMINISTRADOR", "EMPRESA", "OPERARIO"],
        "revision"     : ["ADMINISTRADOR", "OPERARIO", "FONDEADOR"],
        "firma-subida" : ["ADMINISTRADOR", "OPERARIO"],
        "firma-guardado": ["ADMINISTRADOR", "EMPRESA"]
    ]

    def setup() {
        securityProperties.getRol() >> rolConfig
    }

    def buildAuth(String rol) {
        new UsernamePasswordAuthenticationToken("user@test.com", null, [new SimpleGrantedAuthority(rol)])
    }

    @Unroll
    def "hasRol - returns true when '#rol' belongs to group '#group'"() {
        expect:
        rolValidator.hasRol(buildAuth(rol), group)

        where:
        rol              | group
        "ADMINISTRADOR"  | "todos"
        "EMPRESA"        | "todos"
        "OPERARIO"       | "todos"
        "FONDEADOR"      | "todos"
        "ADMINISTRADOR"  | "carga"
        "EMPRESA"        | "carga"
        "OPERARIO"       | "carga"
        "ADMINISTRADOR"  | "revision"
        "OPERARIO"       | "revision"
        "FONDEADOR"      | "revision"
        "ADMINISTRADOR"  | "firma-subida"
        "OPERARIO"       | "firma-subida"
        "ADMINISTRADOR"  | "firma-guardado"
        "EMPRESA"        | "firma-guardado"
    }

    @Unroll
    def "hasRol - returns false when '#rol' does not belong to group '#group'"() {
        expect:
        !rolValidator.hasRol(buildAuth(rol), group)

        where:
        rol         | group
        "FONDEADOR" | "carga"
        "EMPRESA"   | "revision"
        "FONDEADOR" | "firma-subida"
        "OPERARIO"  | "firma-guardado"
        "FONDEADOR" | "firma-guardado"
    }

    def "hasRol - returns false when group does not exist in configuration"() {
        expect:
        !rolValidator.hasRol(buildAuth("ADMINISTRADOR"), "grupo-inexistente")
    }

    def "hasRol - returns false when group is null in configuration"() {
        given:
        def freshProps = Mock(SecurityProperties)
        freshProps.getRol() >> ["carga": null]
        def validator = new RolValidator(freshProps)

        expect:
        !validator.hasRol(buildAuth("ADMINISTRADOR"), "carga")
    }

    def "hasRol - returns false when group has empty list"() {
        given:
        def freshProps = Mock(SecurityProperties)
        freshProps.getRol() >> ["carga": []]
        def validator = new RolValidator(freshProps)

        expect:
        !validator.hasRol(buildAuth("ADMINISTRADOR"), "carga")
    }
}