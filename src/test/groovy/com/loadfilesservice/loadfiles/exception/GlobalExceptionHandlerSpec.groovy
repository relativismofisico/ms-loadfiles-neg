package com.loadfilesservice.loadfiles.exception

import com.loadfilesservice.loadfiles.application.exception.BadRequestException
import com.loadfilesservice.loadfiles.application.exception.InternalServerErrorException
import com.loadfilesservice.loadfiles.application.exception.ResourceNotFoundException
import com.loadfilesservice.loadfiles.web.exception.GlobalExceptionHandler
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.context.request.WebRequest
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification

class GlobalExceptionHandlerSpec extends Specification {

    GlobalExceptionHandler handler = new GlobalExceptionHandler()
    WebRequest request = Stub(WebRequest) {
        getDescription(false) >> "uri=/api/test"
    }

    def "handleAuthenticationException - returns 401"() {
        given:
        def ex = Stub(AuthenticationException) {
            getMessage() >> "Unauthorized"
        }

        when:
        def response = handler.handleAuthenticationException(ex, request)

        then:
        response.statusCode == HttpStatus.UNAUTHORIZED
        response.body.status() == 401
        response.body.path() == "/api/test"
    }

    def "handleAccessDeniedException - returns 403"() {
        given:
        def ex = new AccessDeniedException("Access denied")

        when:
        def response = handler.handleAccessDeniedException(ex, request)

        then:
        response.statusCode == HttpStatus.FORBIDDEN
        response.body.status() == 403
        response.body.message() == "No tiene permisos para acceder a este recurso"
    }

    def "handleResourceNotFound - returns 404 with exception message"() {
        given:
        def ex = new ResourceNotFoundException("Archivo no encontrado")

        when:
        def response = handler.handleResourceNotFound(ex, request)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body.status() == 404
        response.body.message() == "Archivo no encontrado"
    }

    def "handleBadRequest - returns 400 with exception message"() {
        given:
        def ex = new BadRequestException("JSON inválido")

        when:
        def response = handler.handleBadRequest(ex, request)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.status() == 400
        response.body.message() == "JSON inválido"
    }

    def "handleInternalServerError - returns 500 with exception message"() {
        given:
        def ex = new InternalServerErrorException("Error interno")

        when:
        def response = handler.handleInternalServerError(ex, request)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body.status() == 500
        response.body.message() == "Error interno"
    }

    def "handleValidationErrors - returns 422 with field errors"() {
        given:
        def target = new Object()
        def bindingResult = new BeanPropertyBindingResult(target, "target")
        bindingResult.addError(new FieldError("target", "ipLoad", "no debe estar vacío"))
        bindingResult.addError(new FieldError("target", "company", "no debe ser nulo"))
        def ex = new MethodArgumentNotValidException(null, bindingResult)

        when:
        def response = handler.handleValidationErrors(ex, request)

        then:
        response.statusCode == HttpStatus.UNPROCESSABLE_ENTITY
        response.body.status() == 422
        response.body.message().contains("ipLoad")
        response.body.message().contains("company")
    }

    def "handleResponseStatus - returns configured status with reason"() {
        given:
        def ex = new ResponseStatusException(HttpStatus.CONFLICT, "Resource conflict")

        when:
        def response = handler.handleResponseStatus(ex, request)

        then:
        response.statusCode.value() == 409
        response.body.error() == "Conflict"
        response.body.message() == "Resource conflict"
    }

    def "handleResponseStatus - null status resolves to Error"() {
        given:
        def ex = new ResponseStatusException(org.springframework.http.HttpStatusCode.valueOf(999))

        when:
        def response = handler.handleResponseStatus(ex, request)

        then:
        response.body.error() == "Error"
    }

    def "handleResponseStatus - null reason falls back to reason phrase"() {
        given:
        def ex = new ResponseStatusException(HttpStatus.NOT_FOUND)

        when:
        def response = handler.handleResponseStatus(ex, request)

        then:
        response.statusCode.value() == 404
        response.body.message() == "Not Found"
    }

    def "handleGenericException - returns 500 with generic message"() {
        given:
        def ex = new RuntimeException("Unexpected failure")

        when:
        def response = handler.handleGenericException(ex, request)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body.status() == 500
        response.body.message() == "Error interno del servidor"
    }
}