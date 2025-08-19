package com.loadfilesservice.loadfiles.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.loadfilesservice.loadfiles.entity.FileSigned;

public interface IFileSignedDao extends JpaRepository<FileSigned, Long>{
	
	@Query(value =  "select * from db_companies.files_signed where company_ide = ?1 and state = ?2", nativeQuery = true)
	public List<FileSigned> findByCompanyAndState(Long companyId, Long state);

}
