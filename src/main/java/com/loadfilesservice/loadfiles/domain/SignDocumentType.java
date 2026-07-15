package com.loadfilesservice.loadfiles.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

import lombok.Data;

/** Catálogo parametrizable de documentos que requieren firma, independiente de los documentos a cargar. */
@Entity
@Data
@Table(name = "sign_document_type")
public class SignDocumentType implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ide_sign_document_type")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    /** Si es false, el documento no se ofrece más para firma (soft-delete desde la parametrización de administrador). */
    @Column(name = "activo")
    private Boolean activo = Boolean.TRUE;

}
