package com.loadfilesservice.loadfiles.serviceImpl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.loadfilesservice.loadfiles.dao.ICompanyFileDao;
import com.loadfilesservice.loadfiles.entity.CompanyFile;
import com.loadfilesservice.loadfiles.entity.CompanyFileType;
import com.loadfilesservice.loadfiles.exceptions.InternalServerErrorException;
import com.loadfilesservice.loadfiles.exceptions.ResourceNotFoundException;
import com.loadfilesservice.loadfiles.service.ICompanyFileService;
import com.loadfilesservice.loadfiles.service.IFileStorageService;
import com.loadfilesservice.loadfiles.util.ConstantVariables;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyFileServiceImpl implements ICompanyFileService {

	private final ICompanyFileDao companyFileDao;

	private final IFileStorageService fileStorageService;

	@Override
	@Transactional(readOnly = true)
	public Optional<CompanyFile> findById(Long id) {

		if (!companyFileDao.existsById(id)) {
			log.error("[CompanyFileServiceImpl][findById][loadfiles] El archivo no se encuentra en la base de datos");
			throw new ResourceNotFoundException("El archivo no se pudo encontrar en la base de datos");
		}

		return companyFileDao.findById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CompanyFile> findByCompanyAndCompanyFileType(Long companyId, CompanyFileType companyFileType) {
		return companyFileDao.findByCompanyAndCompanyFileType(companyId, companyFileType);
	}

	@Override
	@Transactional
	public CompanyFile save(CompanyFile companyFile) {
		return companyFileDao.save(companyFile);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CompanyFile> findByCompanyAndState(Long companyId, Long state) {
		return companyFileDao.findByCompanyAndState(companyId, state);
	}

	@Override
	@Transactional
	public CompanyFile replaceCompanyFile(MultipartFile file, CompanyFile companyFileBase) {

		List<CompanyFile> existing = companyFileDao.findByCompanyAndCompanyFileType(
				companyFileBase.getCompany(), companyFileBase.getCompanyFileType());

		CompanyFile oldCompanyFile = null;
		for (CompanyFile companyFile : existing) {
			if (companyFile.getState() == 1) {
				oldCompanyFile = companyFile;
			}
		}

		if (oldCompanyFile != null) {
			oldCompanyFile.setState(0L);

			try {
				companyFileDao.save(oldCompanyFile);
			} catch (Exception e) {
				log.error("[CompanyFileServiceImpl][replaceCompanyFile][loadfiles] Error al intentar actualizar el registro del archivo en la base de datos: {}", oldCompanyFile.getFileName());
				throw new InternalServerErrorException("Error al intentar actualizar el registro del archivo en la base de datos: " + oldCompanyFile.getFileName());
			}

			try {
				fileStorageService.deleteFile(oldCompanyFile.getFileName(), ConstantVariables.PATH_UPLOADS);
			} catch (Exception e) {
				log.error("[CompanyFileServiceImpl][replaceCompanyFile][loadfiles] Error al intentar borrar el archivo: {}", oldCompanyFile.getFileName());
				throw new InternalServerErrorException("Error al intentar borrar el archivo: " + oldCompanyFile.getFileName());
			}
		}

		String newFileName;
		try {
			newFileName = fileStorageService.copyFile(file, ConstantVariables.PATH_UPLOADS);
		} catch (IOException e) {
			log.error("[CompanyFileServiceImpl][replaceCompanyFile][loadfiles] Error al intentar guardar el archivo: {}", file.getOriginalFilename());
			throw new InternalServerErrorException("Error al intentar guardar el archivo: " + file.getOriginalFilename());
		}

		companyFileBase.setFileName(newFileName);
		companyFileBase.setOriginalFileName(file.getOriginalFilename());
		companyFileBase.setFilePath(fileStorageService.getPath(newFileName, ConstantVariables.PATH_UPLOADS).toString());
		companyFileBase.setLoadTime(LocalDateTime.now());
		companyFileBase.setState(1L);

		try {
			return companyFileDao.save(companyFileBase);
		} catch (Exception e) {
			log.error("[CompanyFileServiceImpl][replaceCompanyFile][loadfiles] Error al intentar guardar el registro del archivo en la base de datos: {}", newFileName);
			throw new InternalServerErrorException("Error al intentar guardar el registro del archivo en la base de datos: " + newFileName);
		}
	}

}
