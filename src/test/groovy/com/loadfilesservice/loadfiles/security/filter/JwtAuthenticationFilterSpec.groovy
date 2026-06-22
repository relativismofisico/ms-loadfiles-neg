package com.loadfilesservice.loadfiles.security.filter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.loadfilesservice.loadfiles.infraestrutura.security.jwt.JwtAuthenticationFilter
import com.loadfilesservice.loadfiles.infraestrutura.security.jwt.JwtTokenValidator
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Header
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification

import java.io.PrintWriter

class JwtAuthenticationFilterSpec extends Specification {

    JwtTokenValidator jwtTokenValidator = Mock()
    ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenValidator, objectMapper)

    HttpServletRequest request = Mock()
    HttpServletResponse response = Mock()
    FilterChain filterChain = Mock()
    PrintWriter writer = Mock()

    def setup() {
        SecurityContextHolder.clearContext()
        request.getAttribute(_ as String) >> null
        request.getServletPath() >> "/api/test"
        request.getRequestURI() >> "/api/test"
        response.getWriter() >> writer
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    def "doFilter - no Authorization header - passes to chain without authentication"() {
        given:
        request.getHeader(HttpHeaders.AUTHORIZATION) >> null

        when:
        filter.doFilter(request, response, filterChain)

        then:
        1 * filterChain.doFilter(request, response)
        0 * jwtTokenValidator.extractClaims(_)
    }

    def "doFilter - Authorization header without Bearer prefix - passes to chain"() {
        given:
        request.getHeader(HttpHeaders.AUTHORIZATION) >> "Basic sometoken"

        when:
        filter.doFilter(request, response, filterChain)

        then:
        1 * filterChain.doFilter(request, response)
        0 * jwtTokenValidator.extractClaims(_)
    }

    def "doFilter - valid token with username and rol - sets authentication and passes to chain"() {
        given:
        def claims = Mock(Claims)
        claims.getSubject() >> "user@test.com"
        claims.get("rol") >> "ADMINISTRADOR"

        request.getHeader(HttpHeaders.AUTHORIZATION) >> "Bearer validtoken"
        jwtTokenValidator.extractClaims("validtoken") >> claims
        request.getRemoteAddr() >> "127.0.0.1"
        request.getRemoteHost() >> "localhost"
        request.getSession(false) >> null

        when:
        filter.doFilter(request, response, filterChain)

        then:
        1 * filterChain.doFilter(request, response)
        SecurityContextHolder.getContext().getAuthentication() != null
        SecurityContextHolder.getContext().getAuthentication().getPrincipal() == "user@test.com"
    }

    def "doFilter - valid token with null username - does not set authentication"() {
        given:
        def claims = Mock(Claims)
        claims.getSubject() >> null
        claims.get("rol") >> "ADMINISTRADOR"

        request.getHeader(HttpHeaders.AUTHORIZATION) >> "Bearer validtoken"
        jwtTokenValidator.extractClaims("validtoken") >> claims

        when:
        filter.doFilter(request, response, filterChain)

        then:
        1 * filterChain.doFilter(request, response)
        SecurityContextHolder.getContext().getAuthentication() == null
    }

    def "doFilter - valid token with null rol - does not set authentication"() {
        given:
        def claims = Mock(Claims)
        claims.getSubject() >> "user@test.com"
        claims.get("rol") >> null

        request.getHeader(HttpHeaders.AUTHORIZATION) >> "Bearer validtoken"
        jwtTokenValidator.extractClaims("validtoken") >> claims

        when:
        filter.doFilter(request, response, filterChain)

        then:
        1 * filterChain.doFilter(request, response)
        SecurityContextHolder.getContext().getAuthentication() == null
    }

    def "doFilter - expired token - writes 401 response and does not pass to chain"() {
        given:
        def header = Mock(Header)
        def expiredClaims = Mock(Claims)
        request.getHeader(HttpHeaders.AUTHORIZATION) >> "Bearer expiredtoken"
        jwtTokenValidator.extractClaims("expiredtoken") >> {
            throw new ExpiredJwtException(header, expiredClaims, "Token expirado")
        }

        when:
        filter.doFilter(request, response, filterChain)

        then:
        0 * filterChain.doFilter(_, _)
        1 * response.setStatus(401)
    }

    def "doFilter - invalid token throws generic exception - writes 401 response"() {
        given:
        request.getHeader(HttpHeaders.AUTHORIZATION) >> "Bearer invalidtoken"
        jwtTokenValidator.extractClaims("invalidtoken") >> {
            throw new RuntimeException("Invalid token")
        }

        when:
        filter.doFilter(request, response, filterChain)

        then:
        0 * filterChain.doFilter(_, _)
        1 * response.setStatus(401)
    }
}