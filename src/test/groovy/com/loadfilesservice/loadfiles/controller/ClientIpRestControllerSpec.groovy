package com.loadfilesservice.loadfiles.controller

import com.loadfilesservice.loadfiles.web.controller.ClientIpRestController
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class ClientIpRestControllerSpec extends Specification {

    ClientIpRestController controller = new ClientIpRestController()

    MockMvc mockMvc

    def setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    def "getClientIp - sin X-Forwarded-For - retorna la IP remota de la conexión"() {
        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.get("/api/ip"))

        then:
        result.andExpect(status().isOk())
              .andExpect(content().contentType("application/json"))
              .andExpect(jsonPath('$.ip').isNotEmpty())
    }

    def "getClientIp - con X-Forwarded-For - retorna el primer valor de la cabecera"() {
        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/ip")
                .header("X-Forwarded-For", "203.0.113.5, 10.0.0.1")
        )

        then:
        result.andExpect(status().isOk())
              .andExpect(jsonPath('$.ip').value("203.0.113.5"))
    }
}