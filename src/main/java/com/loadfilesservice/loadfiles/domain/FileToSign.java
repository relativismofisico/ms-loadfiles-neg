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

import lombok.Data;

/** Entidad que representa un archivo pendiente de firma. */
@Entity
@Data
@Table(name = "files_to_sign")
public class FileToSign implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "ip_load")
    private String ipLoad;

    @Column(name = "load_time")
    private String loadTime;

    private Long state;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sign_document_type_ide", referencedColumnName = "ide_sign_document_type")
    private SignDocumentType signDocumentType;

}
