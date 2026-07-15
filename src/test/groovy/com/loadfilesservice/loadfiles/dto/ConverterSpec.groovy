package com.loadfilesservice.loadfiles.dto

import com.loadfilesservice.loadfiles.domain.CompanyFile
import com.loadfilesservice.loadfiles.domain.CompanyFileType
import com.loadfilesservice.loadfiles.domain.FileSigned
import com.loadfilesservice.loadfiles.domain.FileToSign
import com.loadfilesservice.loadfiles.domain.SignDocumentType
import com.loadfilesservice.loadfiles.web.dto.CompanyFileDTORequest
import com.loadfilesservice.loadfiles.web.dto.Converter
import com.loadfilesservice.loadfiles.web.dto.FileSignedDTORequest
import spock.lang.Specification

import java.time.LocalDateTime

class ConverterSpec extends Specification {

    Converter converter = new Converter()

    def "companyFileToDTO - maps all fields including loadTime"() {
        given:
        def fileType = new CompanyFileType()
        fileType.id = 1L
        fileType.description = "RUT"
        def companyFile = new CompanyFile()
        companyFile.id = 10L
        companyFile.fileName = "uuid_rut.pdf"
        companyFile.loadTime = LocalDateTime.of(2024, 1, 15, 10, 30)
        companyFile.ipLoad = "192.168.1.1"
        companyFile.company = 5L
        companyFile.companyFileType = fileType

        when:
        def result = converter.companyFileToDTO(companyFile)

        then:
        result.id == 10L
        result.fileName == "uuid_rut.pdf"
        result.loadTime != null
        result.ipLoad == "192.168.1.1"
        result.company == 5L
        result.companyFileType == fileType
    }

    def "companyFileToDTO - null loadTime does not set loadTime in response"() {
        given:
        def companyFile = new CompanyFile()
        companyFile.id = 1L
        companyFile.loadTime = null

        when:
        def result = converter.companyFileToDTO(companyFile)

        then:
        result.id == 1L
        result.loadTime == null
    }

    def "companyFileDTOtoCompanyFile - maps all fields from DTO"() {
        given:
        def fileType = new CompanyFileType()
        fileType.id = 2L
        def dto = new CompanyFileDTORequest()
        dto.ipLoad = "10.0.0.1"
        dto.company = 7L
        dto.companyFileType = fileType

        when:
        def result = converter.companyFileDTOtoCompanyFile(dto)

        then:
        result.ipLoad == "10.0.0.1"
        result.company == 7L
        result.companyFileType == fileType
    }

    def "fileToSignToDTO - maps all fields"() {
        given:
        def signDocumentType = new SignDocumentType()
        signDocumentType.id = 3L
        def fileToSign = new FileToSign()
        fileToSign.id = 20L
        fileToSign.fileName = "tosign.pdf"
        fileToSign.filePath = "/path/to/sign"
        fileToSign.signDocumentType = signDocumentType

        when:
        def result = converter.fileToSignToDTO(fileToSign)

        then:
        result.id == 20L
        result.fileName == "tosign.pdf"
        result.filePath == "/path/to/sign"
        result.signDocumentType == signDocumentType
    }

    def "fileSignedDtoToFileSigned - maps all fields"() {
        given:
        def signDocumentType = new SignDocumentType()
        signDocumentType.id = 4L
        def dto = new FileSignedDTORequest()
        dto.originalFileName = "signed_original.pdf"
        dto.ipLoad = "172.16.0.1"
        dto.company = 3L
        dto.user = 8L
        dto.signDocumentType = signDocumentType

        when:
        def result = converter.fileSignedDtoToFileSigned(dto)

        then:
        result.originalFileName == "signed_original.pdf"
        result.ipLoad == "172.16.0.1"
        result.company == 3L
        result.user == 8L
        result.signDocumentType == signDocumentType
    }
}