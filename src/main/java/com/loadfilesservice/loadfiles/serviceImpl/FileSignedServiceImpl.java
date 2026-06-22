package com.loadfilesservice.loadfiles.serviceImpl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.loadfilesservice.loadfiles.dao.IFileSignedDao;
import com.loadfilesservice.loadfiles.entity.FileSigned;
import com.loadfilesservice.loadfiles.exceptions.InternalServerErrorException;
import com.loadfilesservice.loadfiles.exceptions.ResourceNotFoundException;
import com.loadfilesservice.loadfiles.service.IFileSignedService;
import com.loadfilesservice.loadfiles.service.IFileStorageService;
import com.loadfilesservice.loadfiles.util.ConstantVariables;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileSignedServiceImpl implements IFileSignedService {

	private final IFileSignedDao fileSignedDao;

	private final IFileStorageService fileStorageService;

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
		Optional<FileSigned> result = fileSignedDao.findById(id);
		if (result.isEmpty()) {
			log.error("[FileSignedServiceImpl][findById][loadfiles] El archivo no se encuentra en la base de datos");
			throw new ResourceNotFoundException("El archivo no se pudo encontrar en la base de datos");
		}
		return result;
	}

	@Override
	@Transactional
	public FileSigned saveSignedFile(MultipartFile file, FileSigned fileSignedBase, String companyName) {

		String pathFileSigned = ConstantVariables.FILES_REGISTRY_SIGNED + "/" + companyName;

		fileStorageService.createFolder(pathFileSigned);

		String fileName;
		try {
			fileName = fileStorageService.copyFile(file, pathFileSigned);
		} catch (IOException e) {
			log.error("[FileSignedServiceImpl][saveSignedFile][loadfiles] Error al intentar subir el archivo pdf", e);
			throw new InternalServerErrorException("Error al intentar subir el archivo pdf", e);
		}

		fileSignedBase.setFileName(fileName);
		fileSignedBase.setLoadTime(LocalDateTime.now());
		fileSignedBase.setFilePath(pathFileSigned);
		fileSignedBase.setState(1L);

		try {
			return fileSignedDao.save(fileSignedBase);
		} catch (Exception e) {
			log.error("[FileSignedServiceImpl][saveSignedFile][loadfiles] "
					+ "Error al intentar guardar el registro del archivo en la base de datos: {}",
					fileName, e);
			throw new InternalServerErrorException(
					"Error al intentar guardar el registro del archivo en la base de datos: " + fileName, e);
		}
	}

}
