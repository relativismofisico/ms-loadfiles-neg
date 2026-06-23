package com.loadfilesservice.loadfiles.infraestrutura.persistence;

import java.util.List;

import com.loadfilesservice.loadfiles.domain.CompanyFile;
import com.loadfilesservice.loadfiles.domain.CompanyFileType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

/** Repositorio JPA para la entidad CompanyFile. */
public interface ICompanyFileDao extends CrudRepository<CompanyFile, Long> {

    /** Busca archivos por empresa y tipo de archivo. */
    @Query("select f from CompanyFile f where f.company=?1 and f.companyFileType=?2")
    List<CompanyFile> findByCompanyAndCompanyFileType(Long companyId, CompanyFileType companyFileType);

    /** Busca archivos por empresa y estado usando consulta nativa. */
    @Query(value = "select * from files where company_ide = ?1 and state = ?2", nativeQuery = true)
    List<CompanyFile> findByCompanyAndState(Long companyId, Long state);

}
