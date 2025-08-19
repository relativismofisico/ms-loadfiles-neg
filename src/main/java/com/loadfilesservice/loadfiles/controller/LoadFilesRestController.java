package com.loadfilesservice.loadfiles.controller;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.gson.Gson;
import com.loadfilesservice.loadfiles.dto.CompanyFileDTORequest;
import com.loadfilesservice.loadfiles.dto.CompanyFileDTOResponse;
import com.loadfilesservice.loadfiles.dto.Converter;
import com.loadfilesservice.loadfiles.entity.CompanyFile;
import com.loadfilesservice.loadfiles.entity.CompanyFileType;
import com.loadfilesservice.loadfiles.exceptions.InternalServerErrorException;
import com.loadfilesservice.loadfiles.exceptions.ResourceNotFoundException;
import com.loadfilesservice.loadfiles.service.ICompanyFileService;
import com.loadfilesservice.loadfiles.util.ConstantVariables;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CrossOrigin(origins = { "http://localhost:4300", "http://localhost:4600" })
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class LoadFilesRestController {

	private final ICompanyFileService companyFileService;
	
	private final Converter converter;
	
	@PostMapping(value="/companyfile/upload", produces = "application/json")
	public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile newfile, 
										@RequestParam("fileinfo") String jsonCompanyFile){
		
		Map<String, Object> response = new HashMap<>();
		CompanyFileDTORequest companyFileDTO = new Gson().fromJson(jsonCompanyFile, CompanyFileDTORequest.class);
		
		if (!newfile.isEmpty()) {
			String newFileName = null;
			CompanyFile oldCompanyFile = null;
		
			List<CompanyFile> listCompanyFile = companyFileService.findByCompanyAndCompanyFileType(companyFileDTO.getCompany(), companyFileDTO.getCompanyFileType());
			
			for (CompanyFile companyFile : listCompanyFile) {
				if(companyFile.getState() == 1) {
					oldCompanyFile = companyFile;
				}
			}
			
			if (oldCompanyFile != null) {
				oldCompanyFile.setState((long) 0);
				
				try {
					companyFileService.save(oldCompanyFile);
				} catch (Exception e) {
					log.error("[LoadFilesRestController][uploadFile][loadfiles]" + " Error al intentar actualizar el registro del archivo en la base de datos: " + oldCompanyFile.getFileName());
					throw new InternalServerErrorException("Error al intentar actualizar el registro del archivo en la base de datos: " + oldCompanyFile.getFileName());
				}
				
				try {
					companyFileService.deleteFile(oldCompanyFile.getFileName(), ConstantVariables.PATH_UPLOADS);
				} catch (Exception e) {
					log.error("[LoadFilesRestController][uploadFile][loadfiles]" + " Error al intentar borrar el archivo: " + oldCompanyFile.getFileName());
					throw new InternalServerErrorException("Error al intentar borrar el archivo: " + oldCompanyFile.getFileName());
				}
			}
			
			try {
				newFileName = companyFileService.copyFile(newfile, ConstantVariables.PATH_UPLOADS);
			} catch (Exception e) {
				log.error("[LoadFilesRestController][uploadFile][loadfiles]" + " Error al intentar guardar el archivo: " + newfile.getOriginalFilename());
				throw new InternalServerErrorException("Error al intentar guardar el archivo: " + newfile.getOriginalFilename());
			}
			
			CompanyFile newCompanyFile = converter.companyFileDTOtoCompanyFile(companyFileDTO);
			
			newCompanyFile.setFileName(newFileName);
			newCompanyFile.setOriginalFileName(newfile.getOriginalFilename());
			newCompanyFile.setFilePath(companyFileService.getPath(newFileName, ConstantVariables.PATH_UPLOADS).toString());
			newCompanyFile.setLoadTime(LocalDateTime.now());
			newCompanyFile.setState((long) 1);
			
			System.out.println(newCompanyFile);
			
			try {
				companyFileService.save(newCompanyFile);
			} catch (Exception e) {
				log.error("[LoadFilesRestController][uploadFile][loadfiles]" + " Error al intentar guardar el registro del archivo en la base de datos: " + newFileName);
				throw new InternalServerErrorException("Error al intentar guardar el registro del archivo en la base de datos: " + newFileName);
			}
			
			response.put("savedFile", newCompanyFile);
		}
		
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
	}
	
	@PostMapping(value="/companyfile/upload/file/{id}")
	public ResponseEntity<?> getFile(@Valid @RequestBody CompanyFileType fileType, @PathVariable Long id){
		//Resource resource = null;
		//File fileGetted = null;
		CompanyFile companyFileFounded = null;
		List<CompanyFile> listCompanyFile = null;
		String fileOriginalName = null;
		Map<String, Object> response = new HashMap<>();
		
		try {
			listCompanyFile = companyFileService.findByCompanyAndCompanyFileType(id, fileType);
		} catch (Exception e) {
			log.error("[LoadFilesRestController][getFile][loadfiles]" + " Error al intentar obtener el archivo");
			throw new InternalServerErrorException("Error al intentar obtener el archivo");
		}
		
		
		for (CompanyFile companyFile : listCompanyFile) {
			if(companyFile.getState() == 1) {
				companyFileFounded = companyFile;
			}
		}
		
		if(companyFileFounded != null) {
			
			fileOriginalName = companyFileFounded.getOriginalFileName();
			response.put("fileName", fileOriginalName);
			
			/*try {
				resource = companyFileService.loadFile(companyFileFounded.getFileName());
			} catch (MalformedURLException e) {
				log.error("[LoadFilesRestController][getFile][loadfiles]" + " Error al intentar obtener el archivo: " + companyFileFounded.getFileName());
				throw new InternalServerErrorException("Error al intentar obtener el archivo: " + companyFileFounded.getFileName());
			}*/
		}
		
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.OK);
		
		//if(resource != null) {
			/*HttpHeaders headers = new HttpHeaders();
			headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"");*/
			
			/*try {
				fileGetted = resource.getFile();
			} catch (IOException e) {
				log.error("[LoadFilesRestController][getFile][loadfiles]" + " Error al intentar obtener el archivo del recurso: " + companyFileFounded.getFileName());
				throw new InternalServerErrorException("Error al intentar obtener el archivo del recurso: " + companyFileFounded.getFileName());
			}
			
			return new ResponseEntity<File>(fileGetted, HttpStatus.OK);
		}
		else {
			return new ResponseEntity<File>(fileGetted, HttpStatus.OK);
		}*/
		
	}
	
	@GetMapping(value = "/listofcompanyfiles/{id}", produces = "application/json")
	public ResponseEntity<?> getListOfCompanyFiles(@PathVariable Long id){
		List<CompanyFile> companyFiles = companyFileService.findByCompanyAndState(id, Long.valueOf(1));
		
		if (companyFiles.isEmpty()) {
			log.error("[LoadFilesRestController][getListOfCompanyFiles][loadfiles]" + " No hay archivos de la empresa en la BD");
			throw new ResourceNotFoundException("No hay archivos de la empresa en la BD");
		}
		else {
			List<CompanyFileDTOResponse> companyFilesDTO = companyFiles.stream().map(file -> converter.companyFileToDTO(file))
					.collect(Collectors.toList());
			
			return ResponseEntity.status(HttpStatus.OK).body(companyFilesDTO);
		}
		
	}
	
	@PostMapping("/companyfilepdf")
	public ResponseEntity<byte[]> getPdfCompanyFile(@Valid @RequestBody CompanyFile file)  {
		Resource resource = null;
		File fileGetted = null;
		Optional<CompanyFile> companyFileFounded = null;
		
		try {
			companyFileFounded = companyFileService.findById(file.getId());
		} catch (Exception e) {
			log.error("[LoadFilesRestController][getPdfCompanyFile][ms-loadfiles-neg]" + " Error al intentar obtener el archivo");
			throw new InternalServerErrorException("Error al intentar obtener el archivo");
		}
		
		try {
			resource = companyFileService.loadFile(companyFileFounded.get().getFileName(), ConstantVariables.PATH_UPLOADS);
		} catch (MalformedURLException e) {
			log.error("[LoadFilesRestController][getPdfCompanyFile][ms-loadfiles-neg]" + " Error al intentar obtener el archivo: " + companyFileFounded.get().getFileName());
			throw new InternalServerErrorException("Error al intentar obtener el archivo: " + companyFileFounded.get().getFileName());
		}
		
		try {
			fileGetted = resource.getFile();
		} catch (IOException e) {
			log.error("[LoadFilesRestController][getPdfCompanyFile][ms-loadfiles-neg]" + " Error al intentar obtener el archivo del recurso: " + companyFileFounded.get().getFileName());
			throw new InternalServerErrorException("Error al intentar obtener el archivo del recurso: " + companyFileFounded.get().getFileName());
		}
		
		byte[] pdfBytes = null;
		
		try {
			pdfBytes = Files.readAllBytes(fileGetted.toPath());
		} catch (IOException e) {
			log.error("[LoadFilesRestController][getPdfCompanyFile][ms-loadfiles-neg]" + " Error al intentar los bytes del archivo: " + companyFileFounded.get().getFileName());
			throw new InternalServerErrorException("Error al intentar los bytes del archivo: " + companyFileFounded.get().getFileName());
		}
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.setContentDisposition(ContentDisposition.builder("inline").filename(companyFileFounded.get().getFileName()).build());
		
		return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
		
	}
	
}
