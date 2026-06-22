package com.loadfilesservice.loadfiles.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

/** Entidad que representa una firma digital. */
@Entity
@Data
@Table(name = "sign")
public class Sign implements Serializable {

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
    private LocalDateTime loadTime;

    private Long state;

    @Column(name = "company_ide")
    private Long company;

    @Column(name = "user_ide")
    private Long user;

}
