package com.loadfilesservice.loadfiles.security.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.loadfilesservice.loadfiles.infraestrutura.security.handler.JwtAccessDeniedHandler
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import spock.lang.Specification

import java.io.PrintWriter
import java.io.StringWriter

class JwtAccessDeniedHandlerSpec extends Specification {

    ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    JwtAccessDeniedHandler handler = new JwtAccessDeniedHandler(objectMapper)

    def "handle - writes JSON 403 response"() {
        given:
        def request = Mock(HttpServletRequest)
        def response = Mock(HttpServletResponse)
        def sw = new StringWriter()
        def pw = new PrintWriter(sw)
        def ex = new AccessDeniedException("Forbidden")

        request.getRequestURI() >> "/api/protected"
        response.getWriter() >> pw

        when:
        handler.handle(request, response, ex)

        then:
        1 * response.setStatus(HttpStatus.FORBIDDEN.value())
        1 * response.setContentType("application/json")
        1 * response.setCharacterEncoding("UTF-8")
        sw.toString().contains("403")
        sw.toString().contains("Forbidden")
    }
}