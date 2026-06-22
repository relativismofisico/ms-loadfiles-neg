package com.loadfilesservice.loadfiles.security.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.loadfilesservice.loadfiles.infraestrutura.security.handler.JwtAuthenticationEntryPoint
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.BadCredentialsException
import spock.lang.Specification

import java.io.PrintWriter
import java.io.StringWriter

class JwtAuthenticationEntryPointSpec extends Specification {

    ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(objectMapper)

    def "commence - writes JSON 401 response"() {
        given:
        def request = Mock(HttpServletRequest)
        def response = Mock(HttpServletResponse)
        def sw = new StringWriter()
        def pw = new PrintWriter(sw)
        def ex = new BadCredentialsException("Unauthenticated")

        request.getRequestURI() >> "/api/secure"
        response.getWriter() >> pw

        when:
        entryPoint.commence(request, response, ex)

        then:
        1 * response.setStatus(HttpStatus.UNAUTHORIZED.value())
        1 * response.setContentType("application/json")
        1 * response.setCharacterEncoding("UTF-8")
        sw.toString().contains("401")
        sw.toString().contains("Unauthorized")
    }
}