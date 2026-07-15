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
import com.loadfilesservice.loadfiles.application.service.ICompanyFileService;
import com.loadfilesservice.loadfiles.application.service.IFileStorageService;
import com.loadfilesservice.loadfiles.domain.CompanyFile;
import com.loadfilesservice.loadfiles.domain.CompanyFileType;
import com.loadfilesservice.loadfiles.domain.DocumentReuploadToken;
import com.loadfilesservice.loadfiles.domain.event.DestinatarioEmail;
import com.loadfilesservice.loadfiles.domain.event.NotificacionEmailEvent;
import com.loadfilesservice.loadfiles.infraestrutura.kafka.KafkaProducerService;
import com.loadfilesservice.loadfiles.infraestrutura.persistence.ICompanyFileDao;
import com.loadfilesservice.loadfiles.infraestrutura.persistence.IDocumentReuploadTokenDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Implementación del servicio de archivos de empresa. */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyFileServiceImpl implements ICompanyFileService {

    private static final String REVIEW_STATUS_APROBADO = "APROBADO";
    private static final String REVIEW_STATUS_RECHAZADO = "RECHAZADO";
    private static final String REJECTION_EMAIL_SUBJECT = "Documento rechazado";
    private static final int REUPLOAD_TOKEN_VALID_DAYS = 7;
    // Pendiente mover a application.yml cuando haya URLs por ambiente (mismo criterio pendiente
    // en CompanyNotificationServiceImpl de ms-registroempresa-neg para urlRegistro).
    private static final String REUPLOAD_FRONTEND_BASE_URL = "http://localhost:4300/company/reupload/";

    private final ICompanyFileDao companyFileDao;

    private final IDocumentReuploadTokenDao documentReuploadTokenDao;

    private final IFileStorageService fileStorageService;

    private final KafkaProducerService kafkaProducerService;

    @Override
    @Transactional(readOnly = true)
    public Optional<CompanyFile> findById(Long id) {
        Optional<CompanyFile> result = companyFileDao.findById(id);
        if (result.isEmpty()) {
            log.error("[CompanyFileServiceImpl][findById][loadfiles] El archivo no se encuentra en la base de datos");
            throw new ResourceNotFoundException("El archivo no se pudo encontrar en la base de datos");
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyFile> findByCompanyAndCompanyFileType(Long companyId, CompanyFileType companyFileType) {
        return companyFileDao.findByCompanyAndCompanyFileType(companyId, companyFileType);
    }

    @Override
    @Transactional
    public CompanyFile save(CompanyFile companyFile) {
        return companyFileDao.save(companyFile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyFile> findByCompanyAndState(Long companyId, Long state) {
        return companyFileDao.findByCompanyAndState(companyId, state);
    }

    @Override
    @Transactional
    public CompanyFile replaceCompanyFile(MultipartFile file, CompanyFile companyFileBase) {

        List<CompanyFile> existing = companyFileDao.findByCompanyAndCompanyFileType(
                companyFileBase.getCompany(), companyFileBase.getCompanyFileType());

        CompanyFile oldCompanyFile = existing.stream()
                .filter(cf -> Long.valueOf(1L).equals(cf.getState()))
                .findFirst()
                .orElse(null);

        if (oldCompanyFile != null) {
            oldCompanyFile.setState(0L);

            try {
                companyFileDao.save(oldCompanyFile);
            } catch (Exception e) {
                log.error("[CompanyFileServiceImpl][replaceCompanyFile][loadfiles] "
                        + "Error al intentar actualizar el registro del archivo en la base de datos: {}",
                        oldCompanyFile.getFileName(), e);
                throw new InternalServerErrorException(
                        "Error al intentar actualizar el registro del archivo en la base de datos: "
                                + oldCompanyFile.getFileName(), e);
            }

            try {
                fileStorageService.deleteFile(oldCompanyFile.getFileName(), ConstantVariables.PATH_UPLOADS);
            } catch (Exception e) {
                log.error("[CompanyFileServiceImpl][replaceCompanyFile][loadfiles] "
                        + "Error al intentar borrar el archivo: {}",
                        oldCompanyFile.getFileName());
                throw new InternalServerErrorException(
                        "Error al intentar borrar el archivo: " + oldCompanyFile.getFileName());
            }
        }

        fileStorageService.createFolder(ConstantVariables.PATH_UPLOADS);

        String newFileName;
        try {
            newFileName = fileStorageService.copyFile(file, ConstantVariables.PATH_UPLOADS);
        } catch (IOException e) {
            log.error("[CompanyFileServiceImpl][replaceCompanyFile][loadfiles] "
                    + "Error al intentar guardar el archivo: {}",
                    file.getOriginalFilename(), e);
            throw new InternalServerErrorException(
                    "Error al intentar guardar el archivo: " + file.getOriginalFilename(), e);
        }

        companyFileBase.setFileName(newFileName);
        companyFileBase.setOriginalFileName(file.getOriginalFilename());
        companyFileBase.setFilePath(fileStorageService.getPath(newFileName, ConstantVariables.PATH_UPLOADS).toString());
        companyFileBase.setLoadTime(LocalDateTime.now());
        companyFileBase.setState(1L);

        try {
            return companyFileDao.save(companyFileBase);
        } catch (Exception e) {
            log.error("[CompanyFileServiceImpl][replaceCompanyFile][loadfiles] "
                    + "Error al intentar guardar el registro del archivo en la base de datos: {}",
                    newFileName, e);
            throw new InternalServerErrorException(
                    "Error al intentar guardar el registro del archivo en la base de datos: " + newFileName, e);
        }
    }

    @Override
    @Transactional
    public CompanyFile approve(Long id) {
        CompanyFile companyFile = findById(id).orElseThrow(
                () -> new ResourceNotFoundException("El archivo no se pudo encontrar en la base de datos"));

        companyFile.setReviewStatus(REVIEW_STATUS_APROBADO);
        companyFile.setRejectionReason(null);

        return companyFileDao.save(companyFile);
    }

    @Override
    @Transactional
    public CompanyFile reject(Long id, String rejectionReason, String companyEmail, String companyName) {
        CompanyFile companyFile = findById(id).orElseThrow(
                () -> new ResourceNotFoundException("El archivo no se pudo encontrar en la base de datos"));

        companyFile.setReviewStatus(REVIEW_STATUS_RECHAZADO);
        companyFile.setRejectionReason(rejectionReason);

        CompanyFile saved = companyFileDao.save(companyFile);

        DocumentReuploadToken reuploadToken = createReuploadToken(saved, companyName);

        kafkaProducerService.sendNotificacionEmail(
                buildRejectionEmailEvent(saved, rejectionReason, companyEmail, companyName, reuploadToken));

        return saved;
    }

    private DocumentReuploadToken createReuploadToken(CompanyFile rejectedFile, String companyName) {
        DocumentReuploadToken reuploadToken = new DocumentReuploadToken();
        reuploadToken.setToken(UUID.randomUUID().toString());
        reuploadToken.setRejectedFile(rejectedFile);
        reuploadToken.setCompanyName(companyName);
        reuploadToken.setCreatedAt(LocalDateTime.now());
        reuploadToken.setExpiresAt(LocalDateTime.now().plusDays(REUPLOAD_TOKEN_VALID_DAYS));
        reuploadToken.setUsed(false);

        return documentReuploadTokenDao.save(reuploadToken);
    }

    private NotificacionEmailEvent buildRejectionEmailEvent(
            CompanyFile companyFile, String rejectionReason, String companyEmail, String companyName,
            DocumentReuploadToken reuploadToken) {
        String nombreDocumento = "";
        if (companyFile.getCompanyFileType() != null) {
            nombreDocumento = companyFile.getCompanyFileType().getFileTypeName();
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
                .idNegociacion(String.valueOf(companyFile.getCompany()))
                .tipoNotificacion(REJECTION_EMAIL_SUBJECT)
                .destinatarios(List.of(destinatario))
                .asunto(REJECTION_EMAIL_SUBJECT)
                .data(data)
                .fecha(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentReuploadToken validateReuploadToken(String token) {
        DocumentReuploadToken reuploadToken = documentReuploadTokenDao.findByToken(token)
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
    public CompanyFile redeemReuploadToken(String token, MultipartFile file, String ipLoad) {
        DocumentReuploadToken reuploadToken = validateReuploadToken(token);
        CompanyFile rejectedFile = reuploadToken.getRejectedFile();

        CompanyFile companyFileBase = new CompanyFile();
        companyFileBase.setCompany(rejectedFile.getCompany());
        companyFileBase.setCompanyFileType(rejectedFile.getCompanyFileType());
        companyFileBase.setIpLoad(ipLoad);

        CompanyFile uploaded = replaceCompanyFile(file, companyFileBase);

        reuploadToken.setUsed(true);
        documentReuploadTokenDao.save(reuploadToken);

        return uploaded;
    }

}
