package com.loadfilesservice.loadfiles.usecase

import com.loadfilesservice.loadfiles.application.exception.InternalServerErrorException
import com.loadfilesservice.loadfiles.application.exception.ResourceNotFoundException
import com.loadfilesservice.loadfiles.application.service.ISignDocumentTypeService
import com.loadfilesservice.loadfiles.application.service.IFileStorageService
import com.loadfilesservice.loadfiles.domain.SignDocumentType
import com.loadfilesservice.loadfiles.domain.FileToSign
import com.loadfilesservice.loadfiles.infraestrutura.persistence.IFileToSignDao
import com.loadfilesservice.loadfiles.infraestrutura.usecase.FileToSignServiceImpl
import org.springframework.mock.web.MockMultipartFile
import spock.lang.Specification

class FileToSignServiceImplSpec extends Specification {

    IFileToSignDao fileToSignDao = Mock()
    ISignDocumentTypeService signDocumentTypeService = Mock()
    IFileStorageService fileStorageService = Mock()
    FileToSignServiceImpl service = new FileToSignServiceImpl(fileToSignDao, signDocumentTypeService, fileStorageService)

    def "findById - returns result when file exists"() {
        given:
        def fileToSign = new FileToSign()
        fileToSign.id = 1L
        fileToSign.fileName = "tosign.pdf"
        fileToSignDao.findById(1L) >> Optional.of(fileToSign)

        when:
        def result = service.findById(1L)

        then:
        result.isPresent()
        result.get().id == 1L
        result.get().fileName == "tosign.pdf"
    }

    def "findById - throws ResourceNotFoundException when not found"() {
        given:
        fileToSignDao.findById(99L) >> Optional.empty()

        when:
        service.findById(99L)

        then:
        thrown(ResourceNotFoundException)
    }

    def "findSignableTemplates - delegates to dao with active state"() {
        given:
        def files = [new FileToSign()]
        fileToSignDao.findSignableTemplatesByState(1L) >> files

        when:
        def result = service.findSignableTemplates()

        then:
        result.size() == 1
    }

    def "findAllTemplates - delegates to dao findAll"() {
        given:
        def files = [new FileToSign(), new FileToSign()]
        fileToSignDao.findAll() >> files

        when:
        def result = service.findAllTemplates()

        then:
        result.size() == 2
    }

    def "uploadTemplate - signDocumentType does not exist - throws ResourceNotFoundException"() {
        given:
        signDocumentTypeService.findById(99L) >> Optional.empty()
        def file = new MockMultipartFile("file", "template.pdf", "application/pdf", "content".bytes)

        when:
        service.uploadTemplate(file, 99L, "127.0.0.1")

        then:
        thrown(ResourceNotFoundException)
        0 * fileStorageService.copyFile(*_)
    }

    def "uploadTemplate - no previous template - creates new active template"() {
        given:
        def signDocumentType = new SignDocumentType()
        signDocumentType.id = 10L

        signDocumentTypeService.findById(10L) >> Optional.of(signDocumentType)
        fileToSignDao.findActiveBySignDocumentType(10L) >> Optional.empty()
        fileStorageService.copyFile(_, _) >> "generated-name.pdf"
        fileStorageService.getPath("generated-name.pdf", _) >> java.nio.file.Path.of("generated-name.pdf")
        fileToSignDao.save(_) >> { FileToSign f -> f }

        def file = new MockMultipartFile("file", "template.pdf", "application/pdf", "content".bytes)

        when:
        def result = service.uploadTemplate(file, 10L, "127.0.0.1")

        then:
        result.fileName == "generated-name.pdf"
        result.state == 1L
        result.signDocumentType == signDocumentType
        0 * fileToSignDao.save({ it.state == 0L })
        1 * fileStorageService.createFolder(_)
    }

    def "uploadTemplate - previous template exists - deactivates it before creating new one"() {
        given:
        def signDocumentType = new SignDocumentType()
        signDocumentType.id = 10L

        def previous = new FileToSign()
        previous.id = 5L
        previous.state = 1L

        signDocumentTypeService.findById(10L) >> Optional.of(signDocumentType)
        fileToSignDao.findActiveBySignDocumentType(10L) >> Optional.of(previous)
        fileStorageService.copyFile(_, _) >> "new-file.pdf"
        fileStorageService.getPath("new-file.pdf", _) >> java.nio.file.Path.of("new-file.pdf")
        fileToSignDao.save(_) >> { FileToSign f -> f }

        def file = new MockMultipartFile("file", "template.pdf", "application/pdf", "content".bytes)

        when:
        service.uploadTemplate(file, 10L, "127.0.0.1")

        then:
        1 * fileToSignDao.save({ it.id == 5L && it.state == 0L })
        1 * fileToSignDao.save({ it.fileName == "new-file.pdf" && it.state == 1L })
    }

    def "uploadTemplate - copyFile throws IOException - throws InternalServerErrorException"() {
        given:
        def signDocumentType = new SignDocumentType()
        signDocumentType.id = 10L

        signDocumentTypeService.findById(10L) >> Optional.of(signDocumentType)
        fileToSignDao.findActiveBySignDocumentType(10L) >> Optional.empty()
        fileStorageService.copyFile(_, _) >> { throw new IOException("disk full") }

        def file = new MockMultipartFile("file", "template.pdf", "application/pdf", "content".bytes)

        when:
        service.uploadTemplate(file, 10L, "127.0.0.1")

        then:
        thrown(InternalServerErrorException)
    }
}