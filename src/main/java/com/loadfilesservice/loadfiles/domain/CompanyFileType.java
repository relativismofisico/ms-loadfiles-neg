package com.loadfilesservice.loadfiles.domain;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "file_type")
public class CompanyFileType implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ide_file_type")
    private Long id;

    @Column(name = "description")
    private String description;

    @Column(name = "file_type_name")
    private String fileTypeName;

    private static final long serialVersionUID = 1L;

}
