package com.loadfilesservice.loadfiles.infraestrutura.usecase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.loadfilesservice.loadfiles.application.exception.ResourceNotFoundException;
import com.loadfilesservice.loadfiles.application.service.ISignDocumentTypeService;
import com.loadfilesservice.loadfiles.domain.SignDocumentType;
import com.loadfilesservice.loadfiles.infraestrutura.persistence.ISignDocumentTypeDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementación del servicio del catálogo parametrizable de documentos a firmar. */
@Service
@RequiredArgsConstructor
public class SignDocumentTypeServiceImpl implements ISignDocumentTypeService {

    private final ISignDocumentTypeDao signDocumentTypeDao;

    @Override
    @Transactional(readOnly = true)
    public List<SignDocumentType> findAllActive() {
        return signDocumentTypeDao.findAllByActivoTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SignDocumentType> findAll() {
        List<SignDocumentType> all = new ArrayList<>();
        signDocumentTypeDao.findAll().forEach(all::add);
        return all;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SignDocumentType> findById(Long id) {
        return signDocumentTypeDao.findById(id);
    }

    @Override
    @Transactional
    public SignDocumentType save(SignDocumentType signDocumentType) {
        return signDocumentTypeDao.save(signDocumentType);
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        SignDocumentType signDocumentType = signDocumentTypeDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("El tipo de documento a firmar no existe"));
        signDocumentType.setActivo(Boolean.FALSE);
        signDocumentTypeDao.save(signDocumentType);
    }

}
