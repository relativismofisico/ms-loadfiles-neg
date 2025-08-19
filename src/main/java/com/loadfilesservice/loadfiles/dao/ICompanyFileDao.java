package com.loadfilesservice.loadfiles.dao;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.loadfilesservice.loadfiles.entity.CompanyFile;
import com.loadfilesservice.loadfiles.entity.CompanyFileType;

public interface ICompanyFileDao extends CrudRepository<CompanyFile, Long>{

	@Query("select f from CompanyFile f where f.company=?1 and f.companyFileType=?2")
	public List<CompanyFile> findByCompanyAndCompanyFileType(Long companyId, CompanyFileType companyFileType);
	
	@Query(value =  "select * from db_companies.files where company_ide = ?1 and state = ?2", nativeQuery = true)
	public List<CompanyFile> findByCompanyAndState(Long companyId, Long state);
	
}
