package com.loadfilesservice.loadfiles.usecase

import com.loadfilesservice.loadfiles.application.exception.InternalServerErrorException
import com.loadfilesservice.loadfiles.application.exception.ResourceNotFoundException
import com.loadfilesservice.loadfiles.application.service.IFileStorageService
import com.loadfilesservice.loadfiles.domain.FileSigned
import com.loadfilesservice.loadfiles.infraestrutura.persistence.IFileSignedDao
import com.loadfilesservice.loadfiles.infraestrutura.usecase.FileSignedServiceImpl
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification

class FileSignedServiceImplSpec extends Specification {

    IFileSignedDao fileSignedDao = Mock()
    IFileStorageService fileStorageService = Mock()
    FileSignedServiceImpl service = new FileSignedServiceImpl(fileSignedDao, fileStorageService)

    def "save - delegates to dao"() {
        given:
        def fileSigned = new FileSigned()
        fileSigned.id = 1L

        when:
        def result = service.save(fileSigned)

        then:
        result == fileSigned
        1 * fileSignedDao.save(fileSigned) >> fileSigned
    }

    def "findByCompanyAndState - delegates to dao"() {
        given:
        def files = [new FileSigned(), new FileSigned()]
        fileSignedDao.findByCompanyAndState(10L, 1L) >> files

        when:
        def result = service.findByCompanyAndState(10L, 1L)

        then:
        result.size() == 2
    }

    def "findById - returns result when file exists"() {
        given:
        def fileSigned = new FileSigned()
        fileSigned.id = 1L
        fileSignedDao.findById(1L) >> Optional.of(fileSigned)

        when:
        def result = service.findById(1L)

        then:
        result.isPresent()
        result.get().id == 1L
    }

    def "findById - throws ResourceNotFoundException when not found"() {
        given:
        fileSignedDao.findById(99L) >> Optional.empty()

        when:
        service.findById(99L)

        then:
        thrown(ResourceNotFoundException)
    }

    def "saveSignedFile - happy path"() {
        given:
        def file = Mock(MultipartFile)
        def fileSignedBase = new FileSigned()
        def savedFileSigned = new FileSigned()
        savedFileSigned.id = 1L
        savedFileSigned.fileName = "uuid_signed.pdf"
        savedFileSigned.state = 1L

        fileStorageService.createFolder(_) >> null
        file.getOriginalFilename() >> "signed.pdf"
        fileSignedDao.save(fileSignedBase) >> savedFileSigned

        when:
        def result = service.saveSignedFile(file, fileSignedBase, "TestCompany")

        then:
        result == savedFileSigned
        1 * fileStorageService.createFolder("files_registry_signed/TestCompany")
        1 * fileStorageService.copyFile(file, "files_registry_signed/TestCompany") >> "uuid_signed.pdf"
        fileSignedBase.fileName == "uuid_signed.pdf"
        fileSignedBase.filePath == "files_registry_signed/TestCompany"
        fileSignedBase.state == 1L
        fileSignedBase.loadTime != null
    }

    def "saveSignedFile - copyFile throws IOException - wraps in InternalServerErrorException"() {
        given:
        def file = Mock(MultipartFile)
        def fileSignedBase = new FileSigned()

        fileStorageService.createFolder(_) >> null
        fileStorageService.copyFile(file, _) >> { throw new IOException("copy failed") }

        when:
        service.saveSignedFile(file, fileSignedBase, "TestCompany")

        then:
        thrown(InternalServerErrorException)
    }

    def "saveSignedFile - dao save throws exception - wraps in InternalServerErrorException"() {
        given:
        def file = Mock(MultipartFile)
        def fileSignedBase = new FileSigned()

        fileStorageService.createFolder(_) >> null
        fileStorageService.copyFile(file, _) >> "uuid_signed.pdf"
        fileSignedDao.save(fileSignedBase) >> { throw new RuntimeException("DB error") }

        when:
        service.saveSignedFile(file, fileSignedBase, "TestCompany")

        then:
        thrown(InternalServerErrorException)
    }
}