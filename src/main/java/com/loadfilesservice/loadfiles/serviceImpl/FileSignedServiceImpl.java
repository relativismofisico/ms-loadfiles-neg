package com.loadfilesservice.loadfiles.serviceImpl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loadfilesservice.loadfiles.dao.IFileSignedDao;
import com.loadfilesservice.loadfiles.entity.FileSigned;
import com.loadfilesservice.loadfiles.exceptions.ResourceNotFoundException;
import com.loadfilesservice.loadfiles.service.IFileSignedService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileSignedServiceImpl implements IFileSignedService{
	
	private final IFileSignedDao fileSignedDao;
	
	@Override
	@Transactional
	public FileSigned save(FileSigned fileSigned) {
		return fileSignedDao.save(fileSigned);
	}

	@Override
	@Transactional(readOnly = true)
	public List<FileSigned> findByCompanyAndState(Long companyId, Long state) {
		return fileSignedDao.findByCompanyAndState(companyId, state);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<FileSigned> findById(Long id) {
		
		if (!fileSignedDao.existsById(id)) {
			log.error("[FileSignedServiceImpl][findById][loadfiles]" + " El archivo no se encuentra en la base de datos");
			throw new ResourceNotFoundException("El archivo no se pudo encontrar en la base de datos");
		}
		
		return fileSignedDao.findById(id);
	}

}
