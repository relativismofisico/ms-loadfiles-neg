package com.loadfilesservice.loadfiles.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.loadfilesservice.loadfiles.application.exception.InternalServerErrorException
import com.loadfilesservice.loadfiles.application.exception.ResourceNotFoundException
import com.loadfilesservice.loadfiles.application.service.IFileSignedService
import com.loadfilesservice.loadfiles.application.service.IFileStorageService
import com.loadfilesservice.loadfiles.domain.FileSigned
import com.loadfilesservice.loadfiles.web.controller.FilesSignedRestController
import com.loadfilesservice.loadfiles.web.dto.Converter
import com.loadfilesservice.loadfiles.web.exception.GlobalExceptionHandler
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class FilesSignedRestControllerSpec extends Specification {

    IFileSignedService fileSignedService = Mock()
    IFileStorageService fileStorageService = Mock()
    Converter converter = new Converter()
    ObjectMapper objectMapper = new ObjectMapper()

    FilesSignedRestController controller = new FilesSignedRestController(fileSignedService, fileStorageService, converter)

    MockMvc mockMvc

    def setup() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build()
    }

    // ─── getListOfCompanySignedFiles ──────────────────────────────────────────────

    def "getListOfCompanySignedFiles - files exist - returns 200 with list"() {
        given:
        def file1 = new FileSigned()
        file1.id = 1L
        file1.fileName = "signed1.pdf"
        file1.state = 1L

        fileSignedService.findByCompanyAndState(5L, 1L) >> [file1]

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.get("/filessigned/listofcompanysignedfiles/5")
        )

        then:
        result.andExpect(status().isOk())
    }

    def "getListOfCompanySignedFiles - no files - returns 404"() {
        given:
        fileSignedService.findByCompanyAndState(99L, 1L) >> []

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.get("/filessigned/listofcompanysignedfiles/99")
        )

        then:
        result.andExpect(status().isNotFound())
    }

    // ─── getPdfCompanyFileSigned ──────────────────────────────────────────────────

    def "getPdfCompanyFileSigned - success - returns 200 with PDF bytes"() {
        given:
        def fileSigned = new FileSigned()
        fileSigned.id = 1L
        fileSigned.fileName = "signed.pdf"

        def tempFile = File.createTempFile("signed_pdf", ".pdf")
        tempFile.text = "Signed PDF content"

        def resource = Mock(Resource)
        resource.getFile() >> tempFile

        def requestFile = new FileSigned()
        requestFile.id = 1L

        fileSignedService.findById(1L) >> Optional.of(fileSigned)
        fileStorageService.loadFile("signed.pdf", _) >> resource

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.post("/filessigned/companyfilesignedpdf/TestCompany")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestFile))
        )

        then:
        result.andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_PDF))

        cleanup:
        tempFile.delete()
    }

    def "getPdfCompanyFileSigned - findById throws exception - returns 500"() {
        given:
        def requestFile = new FileSigned()
        requestFile.id = 99L

        fileSignedService.findById(99L) >> { throw new InternalServerErrorException("DB error") }

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.post("/filessigned/companyfilesignedpdf/TestCompany")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestFile))
        )

        then:
        result.andExpect(status().isInternalServerError())
    }

    def "getPdfCompanyFileSigned - loadFile throws MalformedURLException - returns 500"() {
        given:
        def fileSigned = new FileSigned()
        fileSigned.id = 1L
        fileSigned.fileName = "signed.pdf"

        def requestFile = new FileSigned()
        requestFile.id = 1L

        fileSignedService.findById(1L) >> Optional.of(fileSigned)
        fileStorageService.loadFile("signed.pdf", _) >> { throw new java.net.MalformedURLException("bad url") }

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.post("/filessigned/companyfilesignedpdf/TestCompany")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestFile))
        )

        then:
        result.andExpect(status().isInternalServerError())
    }

    def "getPdfCompanyFileSigned - resource.getFile() throws IOException - returns 500"() {
        given:
        def fileSigned = new FileSigned()
        fileSigned.id = 1L
        fileSigned.fileName = "signed.pdf"

        def resource = Mock(Resource)
        resource.getFile() >> { throw new IOException("cannot get file") }

        def requestFile = new FileSigned()
        requestFile.id = 1L

        fileSignedService.findById(1L) >> Optional.of(fileSigned)
        fileStorageService.loadFile("signed.pdf", _) >> resource

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.post("/filessigned/companyfilesignedpdf/TestCompany")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestFile))
        )

        then:
        result.andExpect(status().isInternalServerError())
    }

    def "getPdfCompanyFileSigned - readAllBytes fails (directory) - returns 500"() {
        given:
        def fileSigned = new FileSigned()
        fileSigned.id = 1L
        fileSigned.fileName = "signed.pdf"

        def tempDir = File.createTempFile("signed_dir_trick", ".tmp")
        tempDir.delete()
        tempDir.mkdirs()

        def resource = Mock(Resource)
        resource.getFile() >> tempDir

        def requestFile = new FileSigned()
        requestFile.id = 1L

        fileSignedService.findById(1L) >> Optional.of(fileSigned)
        fileStorageService.loadFile("signed.pdf", _) >> resource

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.post("/filessigned/companyfilesignedpdf/TestCompany")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestFile))
        )

        then:
        result.andExpect(status().isInternalServerError())

        cleanup:
        tempDir.delete()
    }

    // ─── approveFile ────────────────────────────────────────────────────────────

    def "approveFile - returns 200 with the approved document"() {
        given:
        def approved = new FileSigned()
        approved.id = 1L
        approved.reviewStatus = "APROBADO"
        fileSignedService.approve(1L) >> approved

        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.patch("/filessigned/1/approve"))

        then:
        result.andExpect(status().isOk())
              .andExpect(jsonPath('$.reviewStatus').value("APROBADO"))
    }

    def "approveFile - document does not exist - returns 404"() {
        given:
        fileSignedService.approve(99L) >> { throw new ResourceNotFoundException("El archivo no se pudo encontrar en la base de datos") }

        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.patch("/filessigned/99/approve"))

        then:
        result.andExpect(status().isNotFound())
    }

    // ─── rejectFile ─────────────────────────────────────────────────────────────

    def "rejectFile - valid request - returns 200 with the rejected document"() {
        given:
        def rejected = new FileSigned()
        rejected.id = 1L
        rejected.reviewStatus = "RECHAZADO"
        rejected.rejectionReason = "Firma ilegible"
        fileSignedService.reject(1L, "Firma ilegible", "empresa@test.com", "Acme") >> rejected

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.patch("/filessigned/1/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"rejectionReason":"Firma ilegible","companyEmail":"empresa@test.com","companyName":"Acme"}')
        )

        then:
        result.andExpect(status().isOk())
              .andExpect(jsonPath('$.reviewStatus').value("RECHAZADO"))
              .andExpect(jsonPath('$.rejectionReason').value("Firma ilegible"))
    }

}