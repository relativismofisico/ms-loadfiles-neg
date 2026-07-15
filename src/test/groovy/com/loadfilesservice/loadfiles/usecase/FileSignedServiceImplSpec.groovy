package com.loadfilesservice.loadfiles.usecase

import com.loadfilesservice.loadfiles.application.exception.BadRequestException
import com.loadfilesservice.loadfiles.application.exception.InternalServerErrorException
import com.loadfilesservice.loadfiles.application.exception.ResourceNotFoundException
import com.loadfilesservice.loadfiles.application.service.IFileStorageService
import com.loadfilesservice.loadfiles.domain.FileSigned
import com.loadfilesservice.loadfiles.domain.SignDocumentType
import com.loadfilesservice.loadfiles.domain.SignedFileReuploadToken
import com.loadfilesservice.loadfiles.infraestrutura.kafka.KafkaProducerService
import com.loadfilesservice.loadfiles.infraestrutura.persistence.IFileSignedDao
import com.loadfilesservice.loadfiles.infraestrutura.persistence.ISignedFileReuploadTokenDao
import com.loadfilesservice.loadfiles.infraestrutura.usecase.FileSignedServiceImpl
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification

import java.time.LocalDateTime

class FileSignedServiceImplSpec extends Specification {

    IFileSignedDao fileSignedDao = Mock()
    ISignedFileReuploadTokenDao signedFileReuploadTokenDao = Mock()
    IFileStorageService fileStorageService = Mock()
    KafkaProducerService kafkaProducerService = Mock()
    FileSignedServiceImpl service = new FileSignedServiceImpl(
        fileSignedDao, signedFileReuploadTokenDao, fileStorageService, kafkaProducerService)

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

    // ── approve ──────────────────────────────────────────────────────────────

    def "approve - sets APROBADO and clears rejectionReason"() {
        given:
        def fileSigned = new FileSigned()
        fileSigned.id = 1L
        fileSigned.reviewStatus = "RECHAZADO"
        fileSigned.rejectionReason = "Firma ilegible"
        fileSignedDao.findById(1L) >> Optional.of(fileSigned)
        fileSignedDao.save(_) >> { FileSigned f -> f }

        when:
        def result = service.approve(1L)

        then:
        result.reviewStatus == "APROBADO"
        result.rejectionReason == null
    }

    def "approve - throws ResourceNotFoundException when file does not exist"() {
        given:
        fileSignedDao.findById(99L) >> Optional.empty()

        when:
        service.approve(99L)

        then:
        thrown(ResourceNotFoundException)
    }

    // ── reject ───────────────────────────────────────────────────────────────

    def "reject - sets RECHAZADO, creates a reupload token and publishes the rejection email"() {
        given:
        def signDocumentType = new SignDocumentType()
        signDocumentType.name = "Términos y uso de la plataforma"

        def fileSigned = new FileSigned()
        fileSigned.id = 1L
        fileSigned.company = 10L
        fileSigned.signDocumentType = signDocumentType
        fileSignedDao.findById(1L) >> Optional.of(fileSigned)
        fileSignedDao.save(_) >> { FileSigned f -> f }

        when:
        def result = service.reject(1L, "Firma ilegible", "empresa@test.com", "Acme")

        then:
        result.reviewStatus == "RECHAZADO"
        result.rejectionReason == "Firma ilegible"
        1 * signedFileReuploadTokenDao.save({ SignedFileReuploadToken t ->
            t.token != null && t.rejectedFile == fileSigned && t.companyName == "Acme" && t.used == false
        }) >> { SignedFileReuploadToken t -> t }
        1 * kafkaProducerService.sendNotificacionEmail({ event ->
            event.asunto == "Documento firmado rechazado" &&
            event.data.nombreDocumento == "Términos y uso de la plataforma" &&
            event.data.motivoRechazo == "Firma ilegible" &&
            (event.data.linkCarga as String).contains("/company/signed-reupload/")
        })
    }

    def "reject - throws ResourceNotFoundException when file does not exist"() {
        given:
        fileSignedDao.findById(99L) >> Optional.empty()

        when:
        service.reject(99L, "motivo", "empresa@test.com", "Acme")

        then:
        thrown(ResourceNotFoundException)
        0 * kafkaProducerService.sendNotificacionEmail(_)
    }

    // ── validateReuploadToken ────────────────────────────────────────────────

    def "validateReuploadToken - returns the token when valid and unused"() {
        given:
        def token = new SignedFileReuploadToken()
        token.token = "abc"
        token.used = false
        token.expiresAt = LocalDateTime.now().plusDays(1)
        signedFileReuploadTokenDao.findByToken("abc") >> Optional.of(token)

        when:
        def result = service.validateReuploadToken("abc")

        then:
        result == token
    }

    def "validateReuploadToken - throws ResourceNotFoundException when token does not exist"() {
        given:
        signedFileReuploadTokenDao.findByToken("missing") >> Optional.empty()

        when:
        service.validateReuploadToken("missing")

        then:
        thrown(ResourceNotFoundException)
    }

    def "validateReuploadToken - throws BadRequestException when already used"() {
        given:
        def token = new SignedFileReuploadToken()
        token.used = true
        signedFileReuploadTokenDao.findByToken("abc") >> Optional.of(token)

        when:
        service.validateReuploadToken("abc")

        then:
        thrown(BadRequestException)
    }

    def "validateReuploadToken - throws BadRequestException when expired"() {
        given:
        def token = new SignedFileReuploadToken()
        token.used = false
        token.expiresAt = LocalDateTime.now().minusDays(1)
        signedFileReuploadTokenDao.findByToken("abc") >> Optional.of(token)

        when:
        service.validateReuploadToken("abc")

        then:
        thrown(BadRequestException)
    }

    // ── redeemReuploadToken ──────────────────────────────────────────────────

    def "redeemReuploadToken - deactivates the rejected file, uploads the new one and marks the token used"() {
        given:
        def signDocumentType = new SignDocumentType()
        signDocumentType.id = 5L

        def rejectedFile = new FileSigned()
        rejectedFile.id = 1L
        rejectedFile.company = 10L
        rejectedFile.user = 20L
        rejectedFile.signDocumentType = signDocumentType
        rejectedFile.state = 1L

        def token = new SignedFileReuploadToken()
        token.token = "abc"
        token.used = false
        token.expiresAt = LocalDateTime.now().plusDays(1)
        token.rejectedFile = rejectedFile
        token.companyName = "Acme"

        signedFileReuploadTokenDao.findByToken("abc") >> Optional.of(token)
        fileSignedDao.save(_) >> { FileSigned f -> f }
        fileStorageService.createFolder(_) >> null
        fileStorageService.copyFile(_, _) >> "new_signed.pdf"

        def file = Mock(MultipartFile)
        file.getOriginalFilename() >> "corrected.pdf"

        when:
        def result = service.redeemReuploadToken("abc", file, "127.0.0.1")

        then:
        result.fileName == "new_signed.pdf"
        result.state == 1L
        result.company == 10L
        result.user == 20L
        result.signDocumentType == signDocumentType
        rejectedFile.state == 0L
        1 * fileSignedDao.save(rejectedFile)
        token.used == true
        1 * signedFileReuploadTokenDao.save(token)
    }

    def "redeemReuploadToken - throws BadRequestException when token already used"() {
        given:
        def token = new SignedFileReuploadToken()
        token.used = true
        signedFileReuploadTokenDao.findByToken("abc") >> Optional.of(token)
        def file = Mock(MultipartFile)

        when:
        service.redeemReuploadToken("abc", file, "127.0.0.1")

        then:
        thrown(BadRequestException)
        0 * fileStorageService.copyFile(*_)
    }
}
