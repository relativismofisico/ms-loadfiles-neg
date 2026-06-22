package com.loadfilesservice.loadfiles.usecase

import com.loadfilesservice.loadfiles.application.exception.InternalServerErrorException
import com.loadfilesservice.loadfiles.application.exception.ResourceNotFoundException
import com.loadfilesservice.loadfiles.application.service.IFileStorageService
import com.loadfilesservice.loadfiles.domain.CompanyFile
import com.loadfilesservice.loadfiles.domain.CompanyFileType
import com.loadfilesservice.loadfiles.infraestrutura.persistence.ICompanyFileDao
import com.loadfilesservice.loadfiles.infraestrutura.usecase.CompanyFileServiceImpl
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification

class CompanyFileServiceImplSpec extends Specification {

    ICompanyFileDao companyFileDao = Mock()
    IFileStorageService fileStorageService = Mock()
    CompanyFileServiceImpl service = new CompanyFileServiceImpl(companyFileDao, fileStorageService)

    def "findById - returns result when file exists"() {
        given:
        def companyFile = new CompanyFile()
        companyFile.id = 1L
        companyFile.fileName = "test.pdf"
        companyFileDao.findById(1L) >> Optional.of(companyFile)

        when:
        def result = service.findById(1L)

        then:
        result.isPresent()
        result.get().id == 1L
    }

    def "findById - throws ResourceNotFoundException when not found"() {
        given:
        companyFileDao.findById(99L) >> Optional.empty()

        when:
        service.findById(99L)

        then:
        thrown(ResourceNotFoundException)
    }

    def "findByCompanyAndCompanyFileType - delegates to dao"() {
        given:
        def fileType = new CompanyFileType()
        fileType.id = 1L
        def files = [new CompanyFile(), new CompanyFile()]
        companyFileDao.findByCompanyAndCompanyFileType(10L, fileType) >> files

        when:
        def result = service.findByCompanyAndCompanyFileType(10L, fileType)

        then:
        result.size() == 2
    }

    def "save - delegates to dao and returns saved entity"() {
        given:
        def companyFile = new CompanyFile()
        companyFile.id = 1L

        when:
        def result = service.save(companyFile)

        then:
        result == companyFile
        1 * companyFileDao.save(companyFile) >> companyFile
    }

    def "findByCompanyAndState - delegates to dao"() {
        given:
        def files = [new CompanyFile()]
        companyFileDao.findByCompanyAndState(10L, 1L) >> files

        when:
        def result = service.findByCompanyAndState(10L, 1L)

        then:
        result.size() == 1
    }

    def "replaceCompanyFile - no existing active file - happy path"() {
        given:
        def file = Mock(MultipartFile)
        def fileType = new CompanyFileType()
        fileType.id = 1L
        def companyFileBase = new CompanyFile()
        companyFileBase.company = 10L
        companyFileBase.companyFileType = fileType
        def savedFile = new CompanyFile()
        savedFile.id = 5L
        savedFile.fileName = "uuid_test.pdf"
        savedFile.state = 1L

        companyFileDao.findByCompanyAndCompanyFileType(10L, fileType) >> []
        file.getOriginalFilename() >> "test.pdf"
        fileStorageService.getPath("uuid_test.pdf", _) >> java.nio.file.Paths.get("/tmp/uuid_test.pdf")

        when:
        def result = service.replaceCompanyFile(file, companyFileBase)

        then:
        result == savedFile
        1 * fileStorageService.copyFile(file, _) >> "uuid_test.pdf"
        1 * companyFileDao.save(_) >> savedFile
    }

    def "replaceCompanyFile - existing active file - deactivates old and uploads new"() {
        given:
        def file = Mock(MultipartFile)
        def fileType = new CompanyFileType()
        fileType.id = 1L
        def companyFileBase = new CompanyFile()
        companyFileBase.company = 10L
        companyFileBase.companyFileType = fileType
        def existingFile = new CompanyFile()
        existingFile.id = 3L
        existingFile.fileName = "old.pdf"
        existingFile.state = 1L
        def savedFile = new CompanyFile()
        savedFile.id = 5L
        savedFile.fileName = "uuid_new.pdf"
        savedFile.state = 1L

        companyFileDao.findByCompanyAndCompanyFileType(10L, fileType) >> [existingFile]
        companyFileDao.save(existingFile) >> existingFile
        fileStorageService.deleteFile("old.pdf", _) >> true
        file.getOriginalFilename() >> "new.pdf"
        fileStorageService.copyFile(file, _) >> "uuid_new.pdf"
        fileStorageService.getPath("uuid_new.pdf", _) >> java.nio.file.Paths.get("/tmp/uuid_new.pdf")
        companyFileDao.save(companyFileBase) >> savedFile

        when:
        def result = service.replaceCompanyFile(file, companyFileBase)

        then:
        result == savedFile
        existingFile.state == 0L
        1 * companyFileDao.save(existingFile)
        1 * fileStorageService.deleteFile("old.pdf", _)
    }

    def "replaceCompanyFile - existing file with state 0 is not deactivated"() {
        given:
        def file = Mock(MultipartFile)
        def fileType = new CompanyFileType()
        fileType.id = 1L
        def companyFileBase = new CompanyFile()
        companyFileBase.company = 10L
        companyFileBase.companyFileType = fileType
        def inactiveFile = new CompanyFile()
        inactiveFile.id = 3L
        inactiveFile.fileName = "old.pdf"
        inactiveFile.state = 0L
        def savedFile = new CompanyFile()
        savedFile.id = 5L

        companyFileDao.findByCompanyAndCompanyFileType(10L, fileType) >> [inactiveFile]
        file.getOriginalFilename() >> "test.pdf"
        fileStorageService.copyFile(file, _) >> "uuid_test.pdf"
        fileStorageService.getPath("uuid_test.pdf", _) >> java.nio.file.Paths.get("/tmp/uuid_test.pdf")
        companyFileDao.save(_) >> savedFile

        when:
        def result = service.replaceCompanyFile(file, companyFileBase)

        then:
        result == savedFile
        0 * fileStorageService.deleteFile(*_)
    }

    def "replaceCompanyFile - save old file throws exception - wraps in InternalServerErrorException"() {
        given:
        def file = Mock(MultipartFile)
        def fileType = new CompanyFileType()
        fileType.id = 1L
        def companyFileBase = new CompanyFile()
        companyFileBase.company = 10L
        companyFileBase.companyFileType = fileType
        def existingFile = new CompanyFile()
        existingFile.id = 3L
        existingFile.fileName = "old.pdf"
        existingFile.state = 1L

        companyFileDao.findByCompanyAndCompanyFileType(10L, fileType) >> [existingFile]
        companyFileDao.save(existingFile) >> { throw new RuntimeException("DB error") }

        when:
        service.replaceCompanyFile(file, companyFileBase)

        then:
        thrown(InternalServerErrorException)
    }

    def "replaceCompanyFile - deleteFile throws exception - wraps in InternalServerErrorException"() {
        given:
        def file = Mock(MultipartFile)
        def fileType = new CompanyFileType()
        fileType.id = 1L
        def companyFileBase = new CompanyFile()
        companyFileBase.company = 10L
        companyFileBase.companyFileType = fileType
        def existingFile = new CompanyFile()
        existingFile.id = 3L
        existingFile.fileName = "old.pdf"
        existingFile.state = 1L

        companyFileDao.findByCompanyAndCompanyFileType(10L, fileType) >> [existingFile]
        companyFileDao.save(existingFile) >> existingFile
        fileStorageService.deleteFile("old.pdf", _) >> { throw new RuntimeException("delete error") }

        when:
        service.replaceCompanyFile(file, companyFileBase)

        then:
        thrown(InternalServerErrorException)
    }

    def "replaceCompanyFile - copyFile throws IOException - wraps in InternalServerErrorException"() {
        given:
        def file = Mock(MultipartFile)
        def fileType = new CompanyFileType()
        fileType.id = 1L
        def companyFileBase = new CompanyFile()
        companyFileBase.company = 10L
        companyFileBase.companyFileType = fileType

        companyFileDao.findByCompanyAndCompanyFileType(10L, fileType) >> []
        fileStorageService.copyFile(file, _) >> { throw new IOException("copy error") }

        when:
        service.replaceCompanyFile(file, companyFileBase)

        then:
        thrown(InternalServerErrorException)
    }

    def "replaceCompanyFile - final save throws exception - wraps in InternalServerErrorException"() {
        given:
        def file = Mock(MultipartFile)
        def fileType = new CompanyFileType()
        fileType.id = 1L
        def companyFileBase = new CompanyFile()
        companyFileBase.company = 10L
        companyFileBase.companyFileType = fileType

        companyFileDao.findByCompanyAndCompanyFileType(10L, fileType) >> []
        file.getOriginalFilename() >> "test.pdf"
        fileStorageService.copyFile(file, _) >> "uuid_test.pdf"
        fileStorageService.getPath("uuid_test.pdf", _) >> java.nio.file.Paths.get("/tmp/uuid_test.pdf")
        companyFileDao.save(_) >> { throw new RuntimeException("DB error") }

        when:
        service.replaceCompanyFile(file, companyFileBase)

        then:
        thrown(InternalServerErrorException)
    }

    def "replaceCompanyFile - file with null original name - uses uuid_file pattern"() {
        given:
        def file = Mock(MultipartFile)
        def fileType = new CompanyFileType()
        fileType.id = 1L
        def companyFileBase = new CompanyFile()
        companyFileBase.company = 10L
        companyFileBase.companyFileType = fileType
        def savedFile = new CompanyFile()
        savedFile.id = 1L

        companyFileDao.findByCompanyAndCompanyFileType(10L, fileType) >> []
        file.getOriginalFilename() >> null
        fileStorageService.copyFile(file, _) >> "uuid_file"
        fileStorageService.getPath("uuid_file", _) >> java.nio.file.Paths.get("/tmp/uuid_file")
        companyFileDao.save(_) >> savedFile

        when:
        def result = service.replaceCompanyFile(file, companyFileBase)

        then:
        result == savedFile
    }
}