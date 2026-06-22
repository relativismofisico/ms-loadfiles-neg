package com.loadfilesservice.loadfiles.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.loadfilesservice.loadfiles.application.exception.InternalServerErrorException
import com.loadfilesservice.loadfiles.application.service.ICompanyFileService
import com.loadfilesservice.loadfiles.application.service.IFileStorageService
import com.loadfilesservice.loadfiles.domain.CompanyFile
import com.loadfilesservice.loadfiles.domain.CompanyFileType
import com.loadfilesservice.loadfiles.web.controller.LoadFilesRestController
import com.loadfilesservice.loadfiles.web.dto.CompanyFileDTORequest
import com.loadfilesservice.loadfiles.web.dto.Converter
import com.loadfilesservice.loadfiles.web.exception.GlobalExceptionHandler
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class LoadFilesRestControllerSpec extends Specification {

    ICompanyFileService companyFileService = Mock()
    IFileStorageService fileStorageService = Mock()
    Converter converter = new Converter()
    ObjectMapper objectMapper = new ObjectMapper()

    LoadFilesRestController controller = new LoadFilesRestController(
        companyFileService, fileStorageService, converter, objectMapper)

    MockMvc mockMvc

    def setup() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build()
    }

    // ─── uploadFile ───────────────────────────────────────────────────────────────

    def "uploadFile - non-empty file - saves and returns 201"() {
        given:
        def fileType = new CompanyFileType()
        fileType.id = 1L
        def dto = new CompanyFileDTORequest()
        dto.ipLoad = "192.168.1.1"
        dto.company = 1L
        dto.companyFileType = fileType
        def jsonDto = objectMapper.writeValueAsString(dto)
        def savedFile = new CompanyFile()
        savedFile.id = 10L
        savedFile.fileName = "uuid_test.pdf"

        companyFileService.replaceCompanyFile(*_) >> savedFile

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/companyfile/upload")
                .file(new MockMultipartFile("file", "test.pdf", "application/pdf", "content".bytes))
                .param("fileinfo", jsonDto)
        )

        then:
        result.andExpect(status().isCreated())
    }

    def "uploadFile - empty file - returns 201 with empty response"() {
        given:
        def dto = new CompanyFileDTORequest()
        dto.ipLoad = "192.168.1.1"
        dto.company = 1L
        def jsonDto = objectMapper.writeValueAsString(dto)

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/companyfile/upload")
                .file(new MockMultipartFile("file", new byte[0]))
                .param("fileinfo", jsonDto)
        )

        then:
        result.andExpect(status().isCreated())
        0 * companyFileService.replaceCompanyFile(*_)
    }

    def "uploadFile - invalid JSON - returns 400"() {
        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/companyfile/upload")
                .file(new MockMultipartFile("file", "test.pdf", "application/pdf", "content".bytes))
                .param("fileinfo", "not-valid-json{{{")
        )

        then:
        result.andExpect(status().isBadRequest())
    }

    // ─── getFile ──────────────────────────────────────────────────────────────────

    def "getFile - active file exists - returns 200 with filename"() {
        given:
        def fileType = new CompanyFileType()
        fileType.id = 1L
        def companyFile = new CompanyFile()
        companyFile.id = 1L
        companyFile.originalFileName = "rut.pdf"
        companyFile.state = 1L

        companyFileService.findByCompanyAndCompanyFileType(1L, _) >> [companyFile]

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/companyfile/upload/file/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fileType))
        )

        then:
        result.andExpect(status().isOk())
              .andExpect(jsonPath('$.fileName').value("rut.pdf"))
    }

    def "getFile - no active file - returns 200 with empty response"() {
        given:
        def fileType = new CompanyFileType()
        fileType.id = 1L
        def inactiveFile = new CompanyFile()
        inactiveFile.state = 0L

        companyFileService.findByCompanyAndCompanyFileType(1L, _) >> [inactiveFile]

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/companyfile/upload/file/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fileType))
        )

        then:
        result.andExpect(status().isOk())
    }

    def "getFile - service throws exception - returns 500"() {
        given:
        def fileType = new CompanyFileType()
        fileType.id = 1L

        companyFileService.findByCompanyAndCompanyFileType(*_) >> { throw new RuntimeException("DB error") }

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/companyfile/upload/file/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fileType))
        )

        then:
        result.andExpect(status().isInternalServerError())
    }

    // ─── getListOfCompanyFiles ────────────────────────────────────────────────────

    def "getListOfCompanyFiles - files exist - returns 200 with list"() {
        given:
        def fileType = new CompanyFileType()
        fileType.id = 1L
        def file1 = new CompanyFile()
        file1.id = 1L
        file1.fileName = "uuid1.pdf"
        file1.company = 5L
        file1.companyFileType = fileType
        def file2 = new CompanyFile()
        file2.id = 2L
        file2.fileName = "uuid2.pdf"
        file2.company = 5L
        file2.companyFileType = fileType

        companyFileService.findByCompanyAndState(5L, 1L) >> [file1, file2]

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/listofcompanyfiles/5")
        )

        then:
        result.andExpect(status().isOk())
    }

    def "getListOfCompanyFiles - no files - returns 404"() {
        given:
        companyFileService.findByCompanyAndState(99L, 1L) >> []

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/listofcompanyfiles/99")
        )

        then:
        result.andExpect(status().isNotFound())
    }

    // ─── getPdfCompanyFile ────────────────────────────────────────────────────────

    def "getPdfCompanyFile - success - returns 200 with PDF bytes"() {
        given:
        def companyFile = new CompanyFile()
        companyFile.id = 1L
        companyFile.fileName = "uuid_test.pdf"

        def tempFile = File.createTempFile("test_pdf", ".pdf")
        tempFile.text = "PDF content"

        def resource = Mock(Resource)
        resource.getFile() >> tempFile
        resource.exists() >> true

        companyFileService.findById(1L) >> Optional.of(companyFile)
        fileStorageService.loadFile("uuid_test.pdf", _) >> resource

        def requestFile = new CompanyFile()
        requestFile.id = 1L
        requestFile.ipLoad = "127.0.0.1"

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/companyfilepdf")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestFile))
        )

        then:
        result.andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_PDF))

        cleanup:
        tempFile.delete()
    }

    def "getPdfCompanyFile - findById throws exception - returns 500"() {
        given:
        def requestFile = new CompanyFile()
        requestFile.id = 99L
        requestFile.ipLoad = "127.0.0.1"

        companyFileService.findById(99L) >> { throw new InternalServerErrorException("DB error") }

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/companyfilepdf")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestFile))
        )

        then:
        result.andExpect(status().isInternalServerError())
    }

    def "getPdfCompanyFile - loadFile throws MalformedURLException - returns 500"() {
        given:
        def companyFile = new CompanyFile()
        companyFile.id = 1L
        companyFile.fileName = "uuid_test.pdf"

        def requestFile = new CompanyFile()
        requestFile.id = 1L
        requestFile.ipLoad = "127.0.0.1"

        companyFileService.findById(1L) >> Optional.of(companyFile)
        fileStorageService.loadFile("uuid_test.pdf", _) >> { throw new java.net.MalformedURLException("bad url") }

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/companyfilepdf")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestFile))
        )

        then:
        result.andExpect(status().isInternalServerError())
    }

    def "getPdfCompanyFile - resource.getFile() throws IOException - returns 500"() {
        given:
        def companyFile = new CompanyFile()
        companyFile.id = 1L
        companyFile.fileName = "uuid_test.pdf"

        def resource = Mock(Resource)
        resource.getFile() >> { throw new IOException("cannot get file") }

        def requestFile = new CompanyFile()
        requestFile.id = 1L
        requestFile.ipLoad = "127.0.0.1"

        companyFileService.findById(1L) >> Optional.of(companyFile)
        fileStorageService.loadFile("uuid_test.pdf", _) >> resource

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/companyfilepdf")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestFile))
        )

        then:
        result.andExpect(status().isInternalServerError())
    }

    def "getPdfCompanyFile - Files.readAllBytes throws IOException (directory) - returns 500"() {
        given:
        def companyFile = new CompanyFile()
        companyFile.id = 1L
        companyFile.fileName = "uuid_test.pdf"

        def tempDir = File.createTempFile("dir_as_file_trick", ".tmp")
        tempDir.delete()
        tempDir.mkdirs()

        def resource = Mock(Resource)
        resource.getFile() >> tempDir

        def requestFile = new CompanyFile()
        requestFile.id = 1L
        requestFile.ipLoad = "127.0.0.1"

        companyFileService.findById(1L) >> Optional.of(companyFile)
        fileStorageService.loadFile("uuid_test.pdf", _) >> resource

        when:
        def result = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/companyfilepdf")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestFile))
        )

        then:
        result.andExpect(status().isInternalServerError())

        cleanup:
        tempDir.delete()
    }
}