package com.loadfilesservice.loadfiles.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

/** Entidad que representa un archivo firmado. */
@Entity
@Data
@Table(name = "files_signed")
public class FileSigned implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "ip_load")
    private String ipLoad;

    @Column(name = "load_time")
    private LocalDateTime loadTime;

    private Long state;

    @Column(name = "company_ide")
    private Long company;

    @Column(name = "user_ide")
    private Long user;

    // Independiente de "state" (que solo indica si esta es la versión vigente del archivo):
    // PENDIENTE/APROBADO/RECHAZADO. Las filas existentes antes de este campo quedan en NULL;
    // el DTO de respuesta las trata como PENDIENTE (mismo criterio que CompanyFile).
    @Column(name = "review_status")
    private String reviewStatus;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sign_document_type_ide", referencedColumnName = "ide_sign_document_type")
    private SignDocumentType signDocumentType;

}
