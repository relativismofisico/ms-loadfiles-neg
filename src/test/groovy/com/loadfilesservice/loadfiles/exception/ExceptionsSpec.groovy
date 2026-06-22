package com.loadfilesservice.loadfiles.exception

import com.loadfilesservice.loadfiles.application.exception.ApiErrorResponse
import com.loadfilesservice.loadfiles.application.exception.BadRequestException
import com.loadfilesservice.loadfiles.application.exception.InternalServerErrorException
import com.loadfilesservice.loadfiles.application.exception.ResourceNotFoundException
import spock.lang.Specification

import java.time.LocalDateTime

class ExceptionsSpec extends Specification {

    def "BadRequestException - no-arg constructor"() {
        when:
        def ex = new BadRequestException()

        then:
        ex instanceof RuntimeException
        ex.message == null
    }

    def "BadRequestException - message constructor"() {
        when:
        def ex = new BadRequestException("bad input")

        then:
        ex.message == "bad input"
    }

    def "BadRequestException - message and cause constructor"() {
        given:
        def cause = new IllegalArgumentException("root cause")

        when:
        def ex = new BadRequestException("bad input", cause)

        then:
        ex.message == "bad input"
        ex.cause == cause
    }

    def "ResourceNotFoundException - no-arg constructor"() {
        when:
        def ex = new ResourceNotFoundException()

        then:
        ex instanceof RuntimeException
        ex.message == null
    }

    def "ResourceNotFoundException - message constructor"() {
        when:
        def ex = new ResourceNotFoundException("not found")

        then:
        ex.message == "not found"
    }

    def "ResourceNotFoundException - message and cause constructor"() {
        given:
        def cause = new RuntimeException("root")

        when:
        def ex = new ResourceNotFoundException("not found", cause)

        then:
        ex.message == "not found"
        ex.cause == cause
    }

    def "InternalServerErrorException - no-arg constructor"() {
        when:
        def ex = new InternalServerErrorException()

        then:
        ex instanceof RuntimeException
        ex.message == null
    }

    def "InternalServerErrorException - message constructor"() {
        when:
        def ex = new InternalServerErrorException("server error")

        then:
        ex.message == "server error"
    }

    def "InternalServerErrorException - message and cause constructor"() {
        given:
        def cause = new RuntimeException("root")

        when:
        def ex = new InternalServerErrorException("server error", cause)

        then:
        ex.message == "server error"
        ex.cause == cause
    }

    def "ApiErrorResponse.of - creates response with current timestamp"() {
        when:
        def before = LocalDateTime.now()
        def response = ApiErrorResponse.of(404, "Not Found", "Resource missing", "/api/test")
        def after = LocalDateTime.now()

        then:
        response.status() == 404
        response.error() == "Not Found"
        response.message() == "Resource missing"
        response.path() == "/api/test"
        !response.timestamp().isBefore(before)
        !response.timestamp().isAfter(after)
    }
}