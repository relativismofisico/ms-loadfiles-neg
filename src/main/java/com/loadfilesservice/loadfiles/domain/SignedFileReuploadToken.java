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

/**
 * Token de un solo uso enviado por correo cuando se rechaza un documento firmado, para que la
 * empresa pueda volver a cargar el PDF firmado corregido sin iniciar sesión. Mismo patrón que
 * {@link DocumentReuploadToken}, pero referenciando el {@link FileSigned} rechazado.
 */
@Entity
@Data
@Table(name = "signed_file_reupload_tokens")
public class SignedFileReuploadToken implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ide_token")
    private Long id;

    @Column(name = "token", unique = true, nullable = false)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_file_ide", referencedColumnName = "id")
    private FileSigned rejectedFile;

    // Denormalizado: ms-loadfiles-neg no es dueño de los datos de la empresa.
    @Column(name = "company_name")
    private String companyName;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "used")
    private Boolean used;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
