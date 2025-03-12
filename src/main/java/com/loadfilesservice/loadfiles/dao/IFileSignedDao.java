package com.loadfilesservice.loadfiles.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loadfilesservice.loadfiles.entity.FileSigned;

public interface IFileSignedDao extends JpaRepository<FileSigned, Long>{

}
