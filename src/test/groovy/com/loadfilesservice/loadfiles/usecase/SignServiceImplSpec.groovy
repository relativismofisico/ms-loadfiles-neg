package com.loadfilesservice.loadfiles.usecase

import com.loadfilesservice.loadfiles.application.exception.InternalServerErrorException
import com.loadfilesservice.loadfiles.application.service.IFileStorageService
import com.loadfilesservice.loadfiles.domain.Sign
import com.loadfilesservice.loadfiles.infraestrutura.persistence.ISignDao
import com.loadfilesservice.loadfiles.infraestrutura.usecase.SignServiceImpl
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification

class SignServiceImplSpec extends Specification {

    ISignDao signDao = Mock()
    IFileStorageService fileStorageService = Mock()
    SignServiceImpl service = new SignServiceImpl(signDao, fileStorageService)

    def "findActiveSignByCompany - delegates to dao"() {
        given:
        def sign = new Sign()
        sign.id = 1L
        signDao.findActiveSignByCompany(10L) >> Optional.of(sign)

        when:
        def result = service.findActiveSignByCompany(10L)

        then:
        result.isPresent()
        result.get().id == 1L
    }

    def "findActiveSignByUser - delegates to dao"() {
        given:
        def sign = new Sign()
        sign.id = 2L
        signDao.findActiveSignByUser(5L) >> Optional.of(sign)

        when:
        def result = service.findActiveSignByUser(5L)

        then:
        result.isPresent()
        result.get().id == 2L
    }

    def "save - delegates to dao"() {
        given:
        def sign = new Sign()
        sign.id = 1L

        when:
        def result = service.save(sign)

        then:
        result == sign
        1 * signDao.save(sign) >> sign
    }

    def "replaceSign - no existing sign - creates new sign"() {
        given:
        def file = Mock(MultipartFile)
        def savedSign = new Sign()
        savedSign.id = 1L
        savedSign.fileName = "uuid_sign.png"
        savedSign.state = 1L

        fileStorageService.createFolder(_) >> null
        signDao.findActiveSignByCompany(10L) >> Optional.empty()
        file.getOriginalFilename() >> "sign.png"

        when:
        def result = service.replaceSign(file, 10L, 5L, "192.168.1.1", "TestCo")

        then:
        result == savedSign
        1 * fileStorageService.createFolder("signatures/TestCo")
        1 * fileStorageService.copyFile(file, "signatures/TestCo") >> "uuid_sign.png"
        1 * signDao.save(_) >> savedSign
    }

    def "replaceSign - existing sign - deactivates old and creates new"() {
        given:
        def file = Mock(MultipartFile)
        def oldSign = new Sign()
        oldSign.id = 3L
        oldSign.fileName = "old_sign.png"
        oldSign.filePath = "signatures/TestCo"
        oldSign.state = 1L
        def savedSign = new Sign()
        savedSign.id = 4L
        savedSign.state = 1L

        fileStorageService.createFolder(_) >> null
        signDao.findActiveSignByCompany(10L) >> Optional.of(oldSign)
        fileStorageService.deleteFile("old_sign.png", "signatures/TestCo") >> true
        signDao.save(oldSign) >> oldSign
        file.getOriginalFilename() >> "new_sign.png"
        fileStorageService.copyFile(file, _) >> "uuid_new.png"
        signDao.save({ it.id == null }) >> savedSign

        when:
        def result = service.replaceSign(file, 10L, 5L, "192.168.1.1", "TestCo")

        then:
        result == savedSign
        oldSign.state == 0L
        1 * fileStorageService.deleteFile("old_sign.png", "signatures/TestCo")
    }

    def "replaceSign - existing sign - old file not deleted (returns false) - continues"() {
        given:
        def file = Mock(MultipartFile)
        def oldSign = new Sign()
        oldSign.id = 3L
        oldSign.fileName = "old_sign.png"
        oldSign.filePath = "signatures/TestCo"
        oldSign.state = 1L
        def savedSign = new Sign()
        savedSign.id = 4L

        fileStorageService.createFolder(_) >> null
        signDao.findActiveSignByCompany(10L) >> Optional.of(oldSign)
        fileStorageService.deleteFile("old_sign.png", "signatures/TestCo") >> false
        signDao.save(oldSign) >> oldSign
        file.getOriginalFilename() >> "new_sign.png"
        fileStorageService.copyFile(file, _) >> "uuid_new.png"
        signDao.save({ it.id == null }) >> savedSign

        when:
        def result = service.replaceSign(file, 10L, 5L, "192.168.1.1", "TestCo")

        then:
        result == savedSign
    }

    def "replaceSign - findActiveSignByCompany throws exception - wraps in InternalServerErrorException"() {
        given:
        def file = Mock(MultipartFile)

        fileStorageService.createFolder(_) >> null
        signDao.findActiveSignByCompany(10L) >> { throw new RuntimeException("DB error") }

        when:
        service.replaceSign(file, 10L, 5L, "192.168.1.1", "TestCo")

        then:
        thrown(InternalServerErrorException)
    }

    def "replaceSign - save old sign throws exception - wraps in InternalServerErrorException"() {
        given:
        def file = Mock(MultipartFile)
        def oldSign = new Sign()
        oldSign.id = 3L
        oldSign.fileName = "old_sign.png"
        oldSign.filePath = "signatures/TestCo"
        oldSign.state = 1L

        fileStorageService.createFolder(_) >> null
        signDao.findActiveSignByCompany(10L) >> Optional.of(oldSign)
        fileStorageService.deleteFile(*_) >> true
        signDao.save(oldSign) >> { throw new RuntimeException("DB error") }

        when:
        service.replaceSign(file, 10L, 5L, "192.168.1.1", "TestCo")

        then:
        thrown(InternalServerErrorException)
    }

    def "replaceSign - copyFile throws IOException - wraps in InternalServerErrorException"() {
        given:
        def file = Mock(MultipartFile)

        fileStorageService.createFolder(_) >> null
        signDao.findActiveSignByCompany(10L) >> Optional.empty()
        fileStorageService.copyFile(file, _) >> { throw new IOException("copy error") }

        when:
        service.replaceSign(file, 10L, 5L, "192.168.1.1", "TestCo")

        then:
        thrown(InternalServerErrorException)
    }

    def "replaceSign - final save throws exception - wraps in InternalServerErrorException"() {
        given:
        def file = Mock(MultipartFile)

        fileStorageService.createFolder(_) >> null
        signDao.findActiveSignByCompany(10L) >> Optional.empty()
        fileStorageService.copyFile(file, _) >> "uuid_sign.png"
        signDao.save(_) >> { throw new RuntimeException("DB error") }

        when:
        service.replaceSign(file, 10L, 5L, "192.168.1.1", "TestCo")

        then:
        thrown(InternalServerErrorException)
    }

    def "replaceSign - creates Sign with correct properties"() {
        given:
        def file = Mock(MultipartFile)
        Sign capturedSign = null

        fileStorageService.createFolder(_) >> null
        signDao.findActiveSignByCompany(10L) >> Optional.empty()
        fileStorageService.copyFile(file, "signatures/MyCo") >> "uuid_sign.png"
        signDao.save(_) >> { Sign s -> capturedSign = s; s }

        when:
        service.replaceSign(file, 10L, 7L, "10.0.0.1", "MyCo")

        then:
        capturedSign.fileName == "uuid_sign.png"
        capturedSign.filePath == "signatures/MyCo"
        capturedSign.ipLoad == "10.0.0.1"
        capturedSign.company == 10L
        capturedSign.user == 7L
        capturedSign.state == 1L
        capturedSign.loadTime != null
    }
}