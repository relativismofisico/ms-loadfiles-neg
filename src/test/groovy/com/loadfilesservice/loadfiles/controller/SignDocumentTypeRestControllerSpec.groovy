package com.loadfilesservice.loadfiles.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.loadfilesservice.loadfiles.application.exception.ResourceNotFoundException
import com.loadfilesservice.loadfiles.application.service.ISignDocumentTypeService
import com.loadfilesservice.loadfiles.domain.SignDocumentType
import com.loadfilesservice.loadfiles.web.controller.SignDocumentTypeRestController
import com.loadfilesservice.loadfiles.web.exception.GlobalExceptionHandler
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class SignDocumentTypeRestControllerSpec extends Specification {

    ISignDocumentTypeService signDocumentTypeService = Mock()
    ObjectMapper objectMapper = new ObjectMapper()

    SignDocumentTypeRestController controller = new SignDocumentTypeRestController(signDocumentTypeService)

    MockMvc mockMvc

    def setup() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build()
    }

    def "active - returns 200 with the active types"() {
        given:
        def type = new SignDocumentType()
        type.id = 1L
        type.name = "Términos y uso de la plataforma"
        type.activo = true
        signDocumentTypeService.findAllActive() >> [type]

        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.get("/api/signdocumenttypeapi/active"))

        then:
        result.andExpect(status().isOk())
    }

    def "findAll - returns 200 with every type including inactive"() {
        given:
        signDocumentTypeService.findAll() >> [new SignDocumentType(), new SignDocumentType()]

        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.get("/api/signdocumenttypeapi"))

        then:
        result.andExpect(status().isOk())
    }

    def "create - valid request - returns 201"() {
        given:
        def created = new SignDocumentType()
        created.id = 11L
        created.name = "Nuevo documento"
        created.description = "Descripción"
        created.activo = true
        signDocumentTypeService.save(_) >> created

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/signdocumenttypeapi")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"name":"Nuevo documento","description":"Descripción","activo":true}')
        )

        then:
        result.andExpect(status().isCreated())
    }

    def "update - existing type - returns 200"() {
        given:
        def existing = new SignDocumentType()
        existing.id = 1L
        signDocumentTypeService.findById(1L) >> Optional.of(existing)
        signDocumentTypeService.save(_) >> existing

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.put("/api/signdocumenttypeapi/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"name":"Actualizado","description":"Descripción","activo":true}')
        )

        then:
        result.andExpect(status().isOk())
    }

    def "update - type does not exist - returns 404"() {
        given:
        signDocumentTypeService.findById(99L) >> Optional.empty()

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.put("/api/signdocumenttypeapi/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"name":"x","description":"x","activo":true}')
        )

        then:
        result.andExpect(status().isNotFound())
        0 * signDocumentTypeService.save(_)
    }

    def "deactivate - returns 204"() {
        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.delete("/api/signdocumenttypeapi/1"))

        then:
        result.andExpect(status().isNoContent())
        1 * signDocumentTypeService.deactivate(1L)
    }

    def "deactivate - type does not exist - returns 404"() {
        given:
        signDocumentTypeService.deactivate(99L) >> { throw new ResourceNotFoundException("El tipo de documento a firmar no existe") }

        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.delete("/api/signdocumenttypeapi/99"))

        then:
        result.andExpect(status().isNotFound())
    }
}