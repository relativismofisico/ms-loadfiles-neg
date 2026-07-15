package com.loadfilesservice.loadfiles.usecase

import com.loadfilesservice.loadfiles.application.exception.BadRequestException
import com.loadfilesservice.loadfiles.application.exception.InternalServerErrorException
import com.loadfilesservice.loadfiles.application.exception.ResourceNotFoundException
import com.loadfilesservice.loadfiles.application.service.IFileStorageService
import com.loadfilesservice.loadfiles.domain.CompanyFile
import com.loadfilesservice.loadfiles.domain.CompanyFileType
import com.loadfilesservice.loadfiles.domain.DocumentReuploadToken
import com.loadfilesservice.loadfiles.domain.event.NotificacionEmailEvent
import com.loadfilesservice.loadfiles.infraestrutura.kafka.KafkaProducerService
import com.loadfilesservice.loadfiles.infraestrutura.persistence.ICompanyFileDao
import com.loadfilesservice.loadfiles.infraestrutura.persistence.IDocumentReuploadTokenDao
import com.loadfilesservice.loadfiles.infraestrutura.usecase.CompanyFileServiceImpl
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification

class CompanyFileServiceImplSpec extends Specification {

    ICompanyFileDao companyFileDao = Mock()
    IDocumentReuploadTokenDao documentReuploadTokenDao = Mock()
    IFileStorageService fileStorageService = Mock()
    KafkaProducerService kafkaProducerService = Mock()
    CompanyFileServiceImpl service = new CompanyFileServiceImpl(
            companyFileDao, documentReuploadTokenDao, fileStorageService, kafkaProducerService)

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

    def "approve - marks file as APROBADO and clears rejection reason"() {
        given:
        def companyFile = new CompanyFile()
        companyFile.id = 1L
        companyFile.rejectionReason = "motivo previo"
        companyFileDao.findById(1L) >> Optional.of(companyFile)

        when:
        def result = service.approve(1L)

        then:
        result.reviewStatus == "APROBADO"
        result.rejectionReason == null
        1 * companyFileDao.save(companyFile) >> companyFile
        0 * kafkaProducerService.sendNotificacionEmail(_)
    }

    def "approve - throws ResourceNotFoundException when file does not exist"() {
        given:
        companyFileDao.findById(99L) >> Optional.empty()

        when:
        service.approve(99L)

        then:
        thrown(ResourceNotFoundException)
        0 * companyFileDao.save(_)
    }

    def "reject - marks file as RECHAZADO, saves the reason and publishes the notification event"() {
        given:
        def fileType = new CompanyFileType()
        fileType.id = 1L
        fileType.fileTypeName = "Cámara y comercio"
        def companyFile = new CompanyFile()
        companyFile.id = 1L
        companyFile.company = 10L
        companyFile.companyFileType = fileType
        companyFileDao.findById(1L) >> Optional.of(companyFile)
        documentReuploadTokenDao.save(_) >> { DocumentReuploadToken t -> t }

        when:
        def result = service.reject(1L, "Documento borroso", "empresa@correo.com", "Acme S.A.S.")

        then:
        result.reviewStatus == "RECHAZADO"
        result.rejectionReason == "Documento borroso"
        1 * companyFileDao.save(companyFile) >> companyFile
        1 * kafkaProducerService.sendNotificacionEmail({ NotificacionEmailEvent event ->
            event.asunto == "Documento rechazado" &&
                    event.destinatarios[0].tipoActor == "DIRECTO" &&
                    event.destinatarios[0].rutActor == "empresa@correo.com" &&
                    event.data["nombreEmpresa"] == "Acme S.A.S." &&
                    event.data["nombreDocumento"] == "Cámara y comercio" &&
                    event.data["motivoRechazo"] == "Documento borroso" &&
                    (event.data["linkCarga"] as String).startsWith("http://localhost:4300/company/reupload/")
        })
    }

    def "reject - creates a reupload token valid for 7 days linked to the rejected file"() {
        given:
        def companyFile = new CompanyFile()
        companyFile.id = 1L
        companyFile.company = 10L
        companyFileDao.findById(1L) >> Optional.of(companyFile)
        companyFileDao.save(companyFile) >> companyFile

        when:
        service.reject(1L, "Documento borroso", "empresa@correo.com", "Acme S.A.S.")

        then:
        1 * documentReuploadTokenDao.save({ DocumentReuploadToken t ->
            t.rejectedFile == companyFile &&
                    t.companyName == "Acme S.A.S." &&
                    t.used == false &&
                    t.token != null &&
                    t.expiresAt.isAfter(java.time.LocalDateTime.now().plusDays(6))
        }) >> { DocumentReuploadToken t -> t }
    }

    // ─── validateReuploadToken ────────────────────────────────────────────────

    def "validateReuploadToken - returns the token when it exists, is unused and not expired"() {
        given:
        def token = new DocumentReuploadToken(token: "abc", used: false,
                expiresAt: java.time.LocalDateTime.now().plusDays(1))
        documentReuploadTokenDao.findByToken("abc") >> Optional.of(token)

        when:
        def result = service.validateReuploadToken("abc")

        then:
        result == token
    }

    def "validateReuploadToken - throws ResourceNotFoundException when token does not exist"() {
        given:
        documentReuploadTokenDao.findByToken("missing") >> Optional.empty()

        when:
        service.validateReuploadToken("missing")

        then:
        thrown(ResourceNotFoundException)
    }

    def "validateReuploadToken - throws BadRequestException when token was already used"() {
        given:
        def token = new DocumentReuploadToken(token: "abc", used: true,
                expiresAt: java.time.LocalDateTime.now().plusDays(1))
        documentReuploadTokenDao.findByToken("abc") >> Optional.of(token)

        when:
        service.validateReuploadToken("abc")

        then:
        thrown(BadRequestException)
    }

    def "validateReuploadToken - throws BadRequestException when token expired"() {
        given:
        def token = new DocumentReuploadToken(token: "abc", used: false,
                expiresAt: java.time.LocalDateTime.now().minusMinutes(1))
        documentReuploadTokenDao.findByToken("abc") >> Optional.of(token)

        when:
        service.validateReuploadToken("abc")

        then:
        thrown(BadRequestException)
    }

    // ─── redeemReuploadToken ──────────────────────────────────────────────────

    def "redeemReuploadToken - uploads the file for the rejected file's company/type and marks the token used"() {
        given:
        def file = Mock(MultipartFile)
        def fileType = new CompanyFileType()
        fileType.id = 1L
        def rejectedFile = new CompanyFile()
        rejectedFile.id = 5L
        rejectedFile.company = 10L
        rejectedFile.companyFileType = fileType
        def reuploadToken = new DocumentReuploadToken(token: "abc", used: false,
                expiresAt: java.time.LocalDateTime.now().plusDays(1), rejectedFile: rejectedFile)
        documentReuploadTokenDao.findByToken("abc") >> Optional.of(reuploadToken)

        def savedFile = new CompanyFile()
        savedFile.id = 6L
        savedFile.state = 1L

        companyFileDao.findByCompanyAndCompanyFileType(10L, fileType) >> []
        file.getOriginalFilename() >> "corregido.pdf"
        fileStorageService.copyFile(file, _) >> "uuid_corregido.pdf"
        fileStorageService.getPath("uuid_corregido.pdf", _) >> java.nio.file.Paths.get("/tmp/uuid_corregido.pdf")
        companyFileDao.save(_) >> savedFile

        when:
        def result = service.redeemReuploadToken("abc", file, "190.1.2.3")

        then:
        result == savedFile
        reuploadToken.used == true
        1 * documentReuploadTokenDao.save(reuploadToken)
    }

    def "redeemReuploadToken - throws BadRequestException when token already used, without uploading"() {
        given:
        def reuploadToken = new DocumentReuploadToken(token: "abc", used: true,
                expiresAt: java.time.LocalDateTime.now().plusDays(1))
        documentReuploadTokenDao.findByToken("abc") >> Optional.of(reuploadToken)
        def file = Mock(MultipartFile)

        when:
        service.redeemReuploadToken("abc", file, "190.1.2.3")

        then:
        thrown(BadRequestException)
        0 * fileStorageService.copyFile(*_)
    }

    def "reject - throws ResourceNotFoundException when file does not exist"() {
        given:
        companyFileDao.findById(99L) >> Optional.empty()

        when:
        service.reject(99L, "motivo", "empresa@correo.com", "Acme S.A.S.")

        then:
        thrown(ResourceNotFoundException)
        0 * companyFileDao.save(_)
        0 * kafkaProducerService.sendNotificacionEmail(_)
    }
}