package com.loadfilesservice.loadfiles.infraestrutura.usecase;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.loadfilesservice.loadfiles.application.ConstantVariables;
import com.loadfilesservice.loadfiles.application.exception.BadRequestException;
import com.loadfilesservice.loadfiles.application.exception.InternalServerErrorException;
import com.loadfilesservice.loadfiles.application.exception.ResourceNotFoundException;
import com.loadfilesservice.loadfiles.application.service.IFileSignedService;
import com.loadfilesservice.loadfiles.application.service.IFileStorageService;
import com.loadfilesservice.loadfiles.domain.FileSigned;
import com.loadfilesservice.loadfiles.domain.SignedFileReuploadToken;
import com.loadfilesservice.loadfiles.domain.event.DestinatarioEmail;
import com.loadfilesservice.loadfiles.domain.event.NotificacionEmailEvent;
import com.loadfilesservice.loadfiles.infraestrutura.kafka.KafkaProducerService;
import com.loadfilesservice.loadfiles.infraestrutura.persistence.IFileSignedDao;
import com.loadfilesservice.loadfiles.infraestrutura.persistence.ISignedFileReuploadTokenDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Implementación del servicio de archivos firmados. */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileSignedServiceImpl implements IFileSignedService {

    private static final String REVIEW_STATUS_APROBADO = "APROBADO";
    private static final String REVIEW_STATUS_RECHAZADO = "RECHAZADO";
    private static final String REJECTION_EMAIL_SUBJECT = "Documento firmado rechazado";
    private static final int REUPLOAD_TOKEN_VALID_DAYS = 7;
    // Pendiente mover a application.yml, mismo criterio pendiente en CompanyFileServiceImpl.
    private static final String REUPLOAD_FRONTEND_BASE_URL = "http://localhost:4300/company/signed-reupload/";

    private final IFileSignedDao fileSignedDao;

    private final ISignedFileReuploadTokenDao signedFileReuploadTokenDao;

    private final IFileStorageService fileStorageService;

    private final KafkaProducerService kafkaProducerService;

    @Override
    @Transactional
    public FileSigned save(FileSigned fileSigned) {
        return fileSignedDao.save(fileSigned);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileSigned> findByCompanyAndState(Long companyId, Long state) {
        return fileSignedDao.findByCompanyAndState(companyId, state);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FileSigned> findById(Long id) {
        Optional<FileSigned> result = fileSignedDao.findById(id);
        if (result.isEmpty()) {
            log.error("[FileSignedServiceImpl][findById][loadfiles] El archivo no se encuentra en la base de datos");
            throw new ResourceNotFoundException("El archivo no se pudo encontrar en la base de datos");
        }
        return result;
    }

    @Override
    @Transactional
    public FileSigned saveSignedFile(MultipartFile file, FileSigned fileSignedBase, String companyName) {

        String pathFileSigned = ConstantVariables.FILES_REGISTRY_SIGNED + "/" + companyName;

        fileStorageService.createFolder(pathFileSigned);

        String fileName;
        try {
            fileName = fileStorageService.copyFile(file, pathFileSigned);
        } catch (IOException e) {
            log.error("[FileSignedServiceImpl][saveSignedFile][loadfiles] Error al intentar subir el archivo pdf", e);
            throw new InternalServerErrorException("Error al intentar subir el archivo pdf", e);
        }

        fileSignedBase.setFileName(fileName);
        fileSignedBase.setLoadTime(LocalDateTime.now());
        fileSignedBase.setFilePath(pathFileSigned);
        fileSignedBase.setState(1L);

        try {
            return fileSignedDao.save(fileSignedBase);
        } catch (Exception e) {
            log.error("[FileSignedServiceImpl][saveSignedFile][loadfiles] "
                    + "Error al intentar guardar el registro del archivo en la base de datos: {}",
                    fileName, e);
            throw new InternalServerErrorException(
                    "Error al intentar guardar el registro del archivo en la base de datos: " + fileName, e);
        }
    }

    @Override
    @Transactional
    public FileSigned approve(Long id) {
        FileSigned fileSigned = findById(id).orElseThrow(
                () -> new ResourceNotFoundException("El archivo no se pudo encontrar en la base de datos"));

        fileSigned.setReviewStatus(REVIEW_STATUS_APROBADO);
        fileSigned.setRejectionReason(null);

        return fileSignedDao.save(fileSigned);
    }

    @Override
    @Transactional
    public FileSigned reject(Long id, String rejectionReason, String companyEmail, String companyName) {
        FileSigned fileSigned = findById(id).orElseThrow(
                () -> new ResourceNotFoundException("El archivo no se pudo encontrar en la base de datos"));

        fileSigned.setReviewStatus(REVIEW_STATUS_RECHAZADO);
        fileSigned.setRejectionReason(rejectionReason);

        FileSigned saved = fileSignedDao.save(fileSigned);

        SignedFileReuploadToken reuploadToken = createReuploadToken(saved, companyName);

        kafkaProducerService.sendNotificacionEmail(
                buildRejectionEmailEvent(saved, rejectionReason, companyEmail, companyName, reuploadToken));

        return saved;
    }

    private SignedFileReuploadToken createReuploadToken(FileSigned rejectedFile, String companyName) {
        SignedFileReuploadToken reuploadToken = new SignedFileReuploadToken();
        reuploadToken.setToken(UUID.randomUUID().toString());
        reuploadToken.setRejectedFile(rejectedFile);
        reuploadToken.setCompanyName(companyName);
        reuploadToken.setCreatedAt(LocalDateTime.now());
        reuploadToken.setExpiresAt(LocalDateTime.now().plusDays(REUPLOAD_TOKEN_VALID_DAYS));
        reuploadToken.setUsed(false);

        return signedFileReuploadTokenDao.save(reuploadToken);
    }

    private NotificacionEmailEvent buildRejectionEmailEvent(
            FileSigned fileSigned, String rejectionReason, String companyEmail, String companyName,
            SignedFileReuploadToken reuploadToken) {
        String nombreDocumento = "";
        if (fileSigned.getSignDocumentType() != null) {
            nombreDocumento = fileSigned.getSignDocumentType().getName();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("nombreEmpresa", companyName);
        data.put("nombreDocumento", nombreDocumento);
        data.put("motivoRechazo", rejectionReason);
        data.put("linkCarga", REUPLOAD_FRONTEND_BASE_URL + reuploadToken.getToken());

        DestinatarioEmail destinatario = DestinatarioEmail.builder()
                .tipoActor("DIRECTO")
                .rutActor(companyEmail)
                .build();

        return NotificacionEmailEvent.builder()
                .idEvento(UUID.randomUUID().toString())
                .idNegociacion(String.valueOf(fileSigned.getCompany()))
                .tipoNotificacion(REJECTION_EMAIL_SUBJECT)
                .destinatarios(List.of(destinatario))
                .asunto(REJECTION_EMAIL_SUBJECT)
                .data(data)
                .fecha(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SignedFileReuploadToken validateReuploadToken(String token) {
        SignedFileReuploadToken reuploadToken = signedFileReuploadTokenDao.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("El enlace de carga no existe o ya no es válido"));

        if (Boolean.TRUE.equals(reuploadToken.getUsed())) {
            throw new BadRequestException("Este enlace ya fue utilizado para cargar el documento");
        }

        if (reuploadToken.getExpiresAt() != null && reuploadToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Este enlace ya expiró, contacta al administrador");
        }

        return reuploadToken;
    }

    @Override
    @Transactional
    public FileSigned redeemReuploadToken(String token, MultipartFile file, String ipLoad) {
        SignedFileReuploadToken reuploadToken = validateReuploadToken(token);
        FileSigned rejectedFile = reuploadToken.getRejectedFile();

        rejectedFile.setState(0L);
        fileSignedDao.save(rejectedFile);

        FileSigned fileSignedBase = new FileSigned();
        fileSignedBase.setCompany(rejectedFile.getCompany());
        fileSignedBase.setUser(rejectedFile.getUser());
        fileSignedBase.setSignDocumentType(rejectedFile.getSignDocumentType());
        fileSignedBase.setOriginalFileName(file.getOriginalFilename());
        fileSignedBase.setIpLoad(ipLoad);

        FileSigned uploaded = saveSignedFile(file, fileSignedBase, reuploadToken.getCompanyName());

        reuploadToken.setUsed(true);
        signedFileReuploadTokenDao.save(reuploadToken);

        return uploaded;
    }

}
