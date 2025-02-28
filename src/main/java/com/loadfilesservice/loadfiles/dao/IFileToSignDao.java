package com.loadfilesservice.loadfiles.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.loadfilesservice.loadfiles.entity.FileToSign;

public interface IFileToSignDao extends JpaRepository<FileToSign, Long>{
	
	@Query(value =  "select * from db_companies.files_to_sign where file_type_ide = ?1 and state = ?2", nativeQuery = true)
	public List<FileToSign> findByCompanyFileTypeAndState(Long companyFileType, Long state);

}
