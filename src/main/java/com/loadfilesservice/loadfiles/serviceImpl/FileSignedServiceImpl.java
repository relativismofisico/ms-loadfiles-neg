package com.loadfilesservice.loadfiles.serviceImpl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loadfilesservice.loadfiles.dao.IFileSignedDao;
import com.loadfilesservice.loadfiles.entity.FileSigned;
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

}
