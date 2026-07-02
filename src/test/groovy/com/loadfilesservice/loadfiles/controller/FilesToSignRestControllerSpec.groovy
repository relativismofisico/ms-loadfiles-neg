package com.loadfilesservice.loadfiles.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.loadfilesservice.loadfiles.application.exception.InternalServerErrorException
import com.loadfilesservice.loadfiles.application.exception.ResourceNotFoundException
import com.loadfilesservice.loadfiles.application.service.IFileSignedService
import com.loadfilesservice.loadfiles.application.service.IFileStorageService
import com.loadfilesservice.loadfiles.application.service.IFileToSignService
import com.loadfilesservice.loadfiles.application.service.IPdfFormFillerService
import com.loadfilesservice.loadfiles.domain.FileSigned
import com.loadfilesservice.loadfiles.domain.FileToSign
import com.loadfilesservice.loadfiles.web.controller.FilesToSignRestController
import com.loadfilesservice.loadfiles.web.dto.Converter
import com.loadfilesservice.loadfiles.web.dto.FileSignedDTORequest
import com.loadfilesservice.loadfiles.web.dto.PdfToSignRequestDTO
import com.loadfilesservice.loadfiles.web.exception.GlobalExceptionHandler
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class FilesToSignRestControllerSpec extends Specification {

    IFileToSignService fileToSignService = Mock()
    IFileSignedService fileSignedService = Mock()
    IFileStorageService fileStorageService = Mock()
    IPdfFormFillerService pdfFormFillerService = Mock()
    Converter converter = new Converter()
    ObjectMapper objectMapper = new ObjectMapper()

    FilesToSignRestController controller = new FilesToSignRestController(
        fileToSignService, fileSignedService, fileStorageService, pdfFormFillerService, converter, objectMapper)

    MockMvc mockMvc

    def setup() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build()
    }

    // ─── getFileToSignCompanyRegistry ─────────────────────────────────────────────

    def "getFileToSignCompanyRegistry - files exist - returns 200 with list"() {
        given:
        def file1 = new FileToSign()
        file1.id = 1L
        file1.fileName = "registry.pdf"
        file1.state = 1L

        fileToSignService.findByCompanyFileTypeAndState(10L, 1L) >> [file1]

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.get("/filesign/filestosignregistry")
        )

        then:
        result.andExpect(status().isOk())
    }

    def "getFileToSignCompanyRegistry - no files - returns 404"() {
        given:
        fileToSignService.findByCompanyFileTypeAndState(10L, 1L) >> []

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.get("/filesign/filestosignregistry")
        )

        then:
        result.andExpect(status().isNotFound())
    }

    // ─── getPdfToSign ─────────────────────────────────────────────────────────────

    def "getPdfToSign - success - returns 200 with PDF bytes"() {
        given:
        def fileToSign = new FileToSign()
        fileToSign.id = 1L
        fileToSign.fileName = "tosign.pdf"
        fileToSign.filePath = "/some/path"

        def tempFile = File.createTempFile("tosign_pdf", ".pdf")
        tempFile.text = "PDF to sign content"

        def resource = Mock(Resource)
        resource.getFile() >> tempFile

        def requestFile = new PdfToSignRequestDTO()
        requestFile.id = 1L
        requestFile.companyName = "TestCompany"
        requestFile.nit = "123456789"
        requestFile.representativeName = "John Doe"

        fileToSignService.findById(1L) >> Optional.of(fileToSign)
        fileStorageService.loadFile("tosign.pdf", _) >> resource
        pdfFormFillerService.fill(_, _) >> "filled pdf".bytes

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.post("/filesign/pdftosign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestFile))
        )

        then:
        result.andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_PDF))

        cleanup:
        tempFile.delete()
    }

    def "getPdfToSign - findById throws exception - returns 500"() {
        given:
        def requestFile = new PdfToSignRequestDTO()
        requestFile.id = 99L
        requestFile.companyName = "TestCompany"
        requestFile.nit = "123456789"
        requestFile.representativeName = "John Doe"

        fileToSignService.findById(99L) >> { throw new RuntimeException("DB error") }

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.post("/filesign/pdftosign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestFile))
        )

        then:
        result.andExpect(status().isInternalServerError())
    }

    def "getPdfToSign - findById returns empty - returns 500"() {
        given:
        def requestFile = new PdfToSignRequestDTO()
        requestFile.id = 1L
        requestFile.companyName = "TestCompany"
        requestFile.nit = "123456789"
        requestFile.representativeName = "John Doe"

        fileToSignService.findById(1L) >> Optional.empty()

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.post("/filesign/pdftosign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestFile))
        )

        then:
        result.andExpect(status().isInternalServerError())
    }

    def "getPdfToSign - loadFile throws MalformedURLException - returns 500"() {
        given:
        def fileToSign = new FileToSign()
        fileToSign.id = 1L
        fileToSign.fileName = "tosign.pdf"

        def requestFile = new PdfToSignRequestDTO()
        requestFile.id = 1L
        requestFile.companyName = "TestCompany"
        requestFile.nit = "123456789"
        requestFile.representativeName = "John Doe"

        fileToSignService.findById(1L) >> Optional.of(fileToSign)
        fileStorageService.loadFile("tosign.pdf", _) >> { throw new java.net.MalformedURLException("bad url") }

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.post("/filesign/pdftosign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestFile))
        )

        then:
        result.andExpect(status().isInternalServerError())
    }

    def "getPdfToSign - resource.getFile() throws IOException - returns 500"() {
        given:
        def fileToSign = new FileToSign()
        fileToSign.id = 1L
        fileToSign.fileName = "tosign.pdf"

        def resource = Mock(Resource)
        resource.getFile() >> { throw new IOException("cannot get file") }

        def requestFile = new PdfToSignRequestDTO()
        requestFile.id = 1L
        requestFile.companyName = "TestCompany"
        requestFile.nit = "123456789"
        requestFile.representativeName = "John Doe"

        fileToSignService.findById(1L) >> Optional.of(fileToSign)
        fileStorageService.loadFile("tosign.pdf", _) >> resource

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.post("/filesign/pdftosign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestFile))
        )

        then:
        result.andExpect(status().isInternalServerError())
    }

    def "getPdfToSign - readAllBytes fails (directory) - returns 500"() {
        given:
        def fileToSign = new FileToSign()
        fileToSign.id = 1L
        fileToSign.fileName = "tosign.pdf"

        def tempDir = File.createTempFile("tosign_dir_trick", ".tmp")
        tempDir.delete()
        tempDir.mkdirs()

        def resource = Mock(Resource)
        resource.getFile() >> tempDir

        def requestFile = new PdfToSignRequestDTO()
        requestFile.id = 1L
        requestFile.companyName = "TestCompany"
        requestFile.nit = "123456789"
        requestFile.representativeName = "John Doe"

        fileToSignService.findById(1L) >> Optional.of(fileToSign)
        fileStorageService.loadFile("tosign.pdf", _) >> resource

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.post("/filesign/pdftosign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestFile))
        )

        then:
        result.andExpect(status().isInternalServerError())

        cleanup:
        tempDir.delete()
    }

    // ─── uploadSignedPdf ──────────────────────────────────────────────────────────

    def "uploadSignedPdf - valid file and JSON - returns 201"() {
        given:
        def dto = new FileSignedDTORequest()
        dto.company = 1L
        dto.companyName = "TestCompany"
        dto.ipLoad = "127.0.0.1"
        def jsonDto = objectMapper.writeValueAsString(dto)

        def convertedFileSigned = new FileSigned()

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.multipart("/filesign/uploadsignedpdf")
                .file(new MockMultipartFile("file", "signed.pdf", "application/pdf", "PDF content".bytes))
                .param("fileinfo", jsonDto)
        )

        then:
        result.andExpect(status().isCreated())
        1 * fileSignedService.saveSignedFile(_, _, "TestCompany")
    }

    def "uploadSignedPdf - empty file - returns 400"() {
        given:
        def dto = new FileSignedDTORequest()
        dto.company = 1L
        dto.companyName = "TestCompany"
        def jsonDto = objectMapper.writeValueAsString(dto)

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.multipart("/filesign/uploadsignedpdf")
                .file(new MockMultipartFile("file", new byte[0]))
                .param("fileinfo", jsonDto)
        )

        then:
        result.andExpect(status().isBadRequest())
        0 * fileSignedService.saveSignedFile(*_)
    }

    def "uploadSignedPdf - invalid JSON - returns 400"() {
        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.multipart("/filesign/uploadsignedpdf")
                .file(new MockMultipartFile("file", "signed.pdf", "application/pdf", "content".bytes))
                .param("fileinfo", "not-valid-json{{{")
        )

        then:
        result.andExpect(status().isBadRequest())
    }
}