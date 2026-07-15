package com.loadfilesservice.loadfiles.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

import lombok.Data;

/** Entidad que representa el tipo de archivo de empresa. */
@Entity
@Data
@Table(name = "file_type")
public class CompanyFileType implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ide_file_type")
    private Long id;

    @Column(name = "description")
    private String description;

    @Column(name = "file_type_name")
    private String fileTypeName;

    /** Si es true, el documento se pide siempre sin importar las operaciones de interés de la empresa. */
    @Column(name = "es_base")
    private Boolean esBase = Boolean.FALSE;

    @Column(name = "aplica_factoring")
    private Boolean aplicaFactoring = Boolean.FALSE;

    @Column(name = "aplica_confirming")
    private Boolean aplicaConfirming = Boolean.FALSE;

    @Column(name = "aplica_fondeador")
    private Boolean aplicaFondeador = Boolean.FALSE;

    /** Si es false, el documento no se ofrece más (soft-delete desde la parametrización de administrador). */
    @Column(name = "activo")
    private Boolean activo = Boolean.TRUE;

}
