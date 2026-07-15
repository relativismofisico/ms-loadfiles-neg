package com.loadfilesservice.loadfiles.controller

import com.loadfilesservice.loadfiles.application.exception.BadRequestException
import com.loadfilesservice.loadfiles.application.exception.ResourceNotFoundException
import com.loadfilesservice.loadfiles.application.service.IFileSignedService
import com.loadfilesservice.loadfiles.domain.FileSigned
import com.loadfilesservice.loadfiles.domain.SignDocumentType
import com.loadfilesservice.loadfiles.domain.SignedFileReuploadToken
import com.loadfilesservice.loadfiles.web.controller.SignedFileReuploadRestController
import com.loadfilesservice.loadfiles.web.dto.Converter
import com.loadfilesservice.loadfiles.web.exception.GlobalExceptionHandler
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class SignedFileReuploadRestControllerSpec extends Specification {

    IFileSignedService fileSignedService = Mock()
    Converter converter = new Converter()

    SignedFileReuploadRestController controller = new SignedFileReuploadRestController(fileSignedService, converter)

    MockMvc mockMvc

    def setup() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build()
    }

    // ─── getReuploadInfo ──────────────────────────────────────────────────────────

    def "getReuploadInfo - valid token - returns 200 with info"() {
        given:
        def signDocumentType = new SignDocumentType()
        signDocumentType.name = "Términos y uso de la plataforma"

        def rejectedFile = new FileSigned()
        rejectedFile.signDocumentType = signDocumentType
        rejectedFile.rejectionReason = "Firma ilegible"

        def token = new SignedFileReuploadToken()
        token.companyName = "Acme"
        token.rejectedFile = rejectedFile

        fileSignedService.validateReuploadToken("abc") >> token

        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.get("/api/filesigned/reupload/abc"))

        then:
        result.andExpect(status().isOk())
              .andExpect(jsonPath('$.companyName').value("Acme"))
              .andExpect(jsonPath('$.documentTypeName').value("Términos y uso de la plataforma"))
              .andExpect(jsonPath('$.rejectionReason').value("Firma ilegible"))
    }

    def "getReuploadInfo - token not found - returns 404"() {
        given:
        fileSignedService.validateReuploadToken("missing") >> { throw new ResourceNotFoundException("El enlace de carga no existe o ya no es válido") }

        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.get("/api/filesigned/reupload/missing"))

        then:
        result.andExpect(status().isNotFound())
    }

    def "getReuploadInfo - token already used - returns 400"() {
        given:
        fileSignedService.validateReuploadToken("used") >> { throw new BadRequestException("Este enlace ya fue utilizado para cargar el documento") }

        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.get("/api/filesigned/reupload/used"))

        then:
        result.andExpect(status().isBadRequest())
    }

    // ─── redeemReuploadToken ──────────────────────────────────────────────────────

    def "redeemReuploadToken - valid request - returns 200 with the uploaded document"() {
        given:
        def uploaded = new FileSigned()
        uploaded.id = 5L
        uploaded.fileName = "new_signed.pdf"
        fileSignedService.redeemReuploadToken("abc", _, "127.0.0.1") >> uploaded

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/filesigned/reupload/abc")
                .file(new MockMultipartFile("file", "corrected.pdf", "application/pdf", "PDF content".bytes))
                .param("ipLoad", "127.0.0.1")
        )

        then:
        result.andExpect(status().isOk())
              .andExpect(jsonPath('$.fileName').value("new_signed.pdf"))
    }

    def "redeemReuploadToken - token expired - returns 400"() {
        given:
        fileSignedService.redeemReuploadToken("abc", _, "127.0.0.1") >> { throw new BadRequestException("Este enlace ya expiró, contacta al administrador") }

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/filesigned/reupload/abc")
                .file(new MockMultipartFile("file", "corrected.pdf", "application/pdf", "PDF content".bytes))
                .param("ipLoad", "127.0.0.1")
        )

        then:
        result.andExpect(status().isBadRequest())
    }
}