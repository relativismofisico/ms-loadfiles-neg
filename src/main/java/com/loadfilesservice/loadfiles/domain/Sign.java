package com.loadfilesservice.loadfiles.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "sign")
public class Sign implements Serializable {

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

    private static final long serialVersionUID = 1L;

}
