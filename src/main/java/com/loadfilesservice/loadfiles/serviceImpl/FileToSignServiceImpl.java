package com.loadfilesservice.loadfiles.serviceImpl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loadfilesservice.loadfiles.dao.IFileToSignDao;
import com.loadfilesservice.loadfiles.entity.FileToSign;
import com.loadfilesservice.loadfiles.exceptions.ResourceNotFoundException;
import com.loadfilesservice.loadfiles.service.IFileToSignService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileToSignServiceImpl implements IFileToSignService{
	
	private final IFileToSignDao fileToSignDao;

	@Override
	@Transactional(readOnly = true)
	public List<FileToSign> findByCompanyFileTypeAndState(Long companyFileType, Long state) {
		return fileToSignDao.findByCompanyFileTypeAndState(companyFileType, state);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<FileToSign> findById(Long id) {
		
		if (!fileToSignDao.existsById(id)) {
			log.error("[FileToSignServiceImpl][findById][loadfiles]" + " El archivo no se encuentra en la base de datos");
			throw new ResourceNotFoundException("El archivo no se pudo encontrar en la base de datos");
		}
		
		return fileToSignDao.findById(id);
	}

}
