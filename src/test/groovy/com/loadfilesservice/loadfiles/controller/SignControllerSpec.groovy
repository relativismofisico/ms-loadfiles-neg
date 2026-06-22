package com.loadfilesservice.loadfiles.controller

import com.loadfilesservice.loadfiles.application.exception.InternalServerErrorException
import com.loadfilesservice.loadfiles.application.service.IFileStorageService
import com.loadfilesservice.loadfiles.application.service.ISignService
import com.loadfilesservice.loadfiles.domain.Sign
import com.loadfilesservice.loadfiles.web.controller.SignController
import com.loadfilesservice.loadfiles.web.exception.GlobalExceptionHandler
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class SignControllerSpec extends Specification {

    ISignService signService = Mock()
    IFileStorageService fileStorageService = Mock()

    SignController controller = new SignController(signService, fileStorageService)

    MockMvc mockMvc

    def setup() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build()
    }

    // ─── saveSign ─────────────────────────────────────────────────────────────────

    def "saveSign - valid params with user=0 (null user) - returns 201"() {
        given:
        def savedSign = new Sign()
        savedSign.id = 1L
        savedSign.fileName = "sign.png"

        signService.replaceSign(_, 5L, null, "127.0.0.1", "TestCompany") >> savedSign

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.multipart("/signs/save")
                .file(new MockMultipartFile("file", "sign.png", "image/png", "PNG data".bytes))
                .param("company", "5")
                .param("user", "0")
                .param("ipLoad", "127.0.0.1")
                .param("companyName", "TestCompany")
        )

        then:
        result.andExpect(status().isCreated())
    }

    def "saveSign - valid params with non-zero user - returns 201"() {
        given:
        def savedSign = new Sign()
        savedSign.id = 1L
        savedSign.fileName = "sign.png"

        signService.replaceSign(_, 5L, 42L, "127.0.0.1", "TestCompany") >> savedSign

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.multipart("/signs/save")
                .file(new MockMultipartFile("file", "sign.png", "image/png", "PNG data".bytes))
                .param("company", "5")
                .param("user", "42")
                .param("ipLoad", "127.0.0.1")
                .param("companyName", "TestCompany")
        )

        then:
        result.andExpect(status().isCreated())
    }

    // ─── getActiveSignByCompany ───────────────────────────────────────────────────

    def "getActiveSignByCompany - sign found - returns 200 with resource"() {
        given:
        def sign = new Sign()
        sign.id = 1L
        sign.fileName = "sign.png"
        sign.filePath = "/signs/company"

        def resource = Mock(Resource)

        signService.findActiveSignByCompany(5L) >> Optional.of(sign)
        fileStorageService.loadFile("sign.png", "/signs/company") >> resource

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.get("/signs/company/5")
        )

        then:
        result.andExpect(status().isOk())
    }

    def "getActiveSignByCompany - no sign found - returns 404"() {
        given:
        signService.findActiveSignByCompany(99L) >> Optional.empty()

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.get("/signs/company/99")
        )

        then:
        result.andExpect(status().isNotFound())
    }

    def "getActiveSignByCompany - service throws exception - returns 500"() {
        given:
        signService.findActiveSignByCompany(1L) >> { throw new RuntimeException("DB error") }

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.get("/signs/company/1")
        )

        then:
        result.andExpect(status().isInternalServerError())
    }

    def "getActiveSignByCompany - loadFile throws MalformedURLException - returns 500"() {
        given:
        def sign = new Sign()
        sign.id = 1L
        sign.fileName = "sign.png"
        sign.filePath = "/signs/company"

        signService.findActiveSignByCompany(5L) >> Optional.of(sign)
        fileStorageService.loadFile("sign.png", "/signs/company") >> { throw new java.net.MalformedURLException("bad url") }

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.get("/signs/company/5")
        )

        then:
        result.andExpect(status().isInternalServerError())
    }

    // ─── getActiveSignByUser ──────────────────────────────────────────────────────

    def "getActiveSignByUser - sign found - returns 200 with resource"() {
        given:
        def sign = new Sign()
        sign.id = 1L
        sign.fileName = "user_sign.png"
        sign.filePath = "/signs/user"

        def resource = Mock(Resource)

        signService.findActiveSignByUser(42L) >> Optional.of(sign)
        fileStorageService.loadFile("user_sign.png", "/signs/user") >> resource

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.get("/signs/user/42")
        )

        then:
        result.andExpect(status().isOk())
    }

    def "getActiveSignByUser - no sign found - returns 404"() {
        given:
        signService.findActiveSignByUser(99L) >> Optional.empty()

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.get("/signs/user/99")
        )

        then:
        result.andExpect(status().isNotFound())
    }

    def "getActiveSignByUser - service throws exception - returns 500"() {
        given:
        signService.findActiveSignByUser(1L) >> { throw new RuntimeException("DB error") }

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.get("/signs/user/1")
        )

        then:
        result.andExpect(status().isInternalServerError())
    }

    def "getActiveSignByUser - loadFile throws MalformedURLException - returns 500"() {
        given:
        def sign = new Sign()
        sign.id = 1L
        sign.fileName = "user_sign.png"
        sign.filePath = "/signs/user"

        signService.findActiveSignByUser(42L) >> Optional.of(sign)
        fileStorageService.loadFile("user_sign.png", "/signs/user") >> { throw new java.net.MalformedURLException("bad url") }

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.get("/signs/user/42")
        )

        then:
        result.andExpect(status().isInternalServerError())
    }
}