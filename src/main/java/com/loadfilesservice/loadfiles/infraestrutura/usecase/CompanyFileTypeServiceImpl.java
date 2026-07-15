package com.loadfilesservice.loadfiles.infraestrutura.usecase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.loadfilesservice.loadfiles.application.exception.ResourceNotFoundException;
import com.loadfilesservice.loadfiles.application.service.ICompanyFileTypeService;
import com.loadfilesservice.loadfiles.domain.CompanyFileType;
import com.loadfilesservice.loadfiles.infraestrutura.persistence.ICompanyFileTypeDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementación del servicio del catálogo parametrizable de tipos de documento. */
@Service
@RequiredArgsConstructor
public class CompanyFileTypeServiceImpl implements ICompanyFileTypeService {

    private final ICompanyFileTypeDao companyFileTypeDao;

    @Override
    @Transactional(readOnly = true)
    public List<CompanyFileType> findAllActive() {
        return companyFileTypeDao.findAllByActivoTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyFileType> findAll() {
        List<CompanyFileType> all = new ArrayList<>();
        companyFileTypeDao.findAll().forEach(all::add);
        return all;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyFileType> findApplicable(boolean factoring, boolean confirming, boolean fondeador) {
        return companyFileTypeDao.findApplicable(factoring, confirming, fondeador);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CompanyFileType> findById(Long id) {
        return companyFileTypeDao.findById(id);
    }

    @Override
    @Transactional
    public CompanyFileType save(CompanyFileType companyFileType) {
        return companyFileTypeDao.save(companyFileType);
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        CompanyFileType companyFileType = companyFileTypeDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("El tipo de documento no existe"));
        companyFileType.setActivo(Boolean.FALSE);
        companyFileTypeDao.save(companyFileType);
    }

}
