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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.google.gson.Gson;
import com.loadfilesservice.loadfiles.dto.Converter;
import com.loadfilesservice.loadfiles.dto.FileSignedDTORequest;
import com.loadfilesservice.loadfiles.dto.FileToSignDTOResponse;
import com.loadfilesservice.loadfiles.entity.FileSigned;
import com.loadfilesservice.loadfiles.entity.FileToSign;
import com.loadfilesservice.loadfiles.exceptions.BadRequestException;
import com.loadfilesservice.loadfiles.exceptions.InternalServerErrorException;
import com.loadfilesservice.loadfiles.service.ICompanyFileService;
import com.loadfilesservice.loadfiles.service.IFileSignedService;
import com.loadfilesservice.loadfiles.service.IFileToSignService;
import com.loadfilesservice.loadfiles.util.ConstantVariables;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CrossOrigin(origins = { "http://localhost:4200", "http://localhost:4300" })
@RestController
@RequestMapping("/filesign")
@RequiredArgsConstructor
@Slf4j
public class FilesToSignRestController {
	
	private final IFileToSignService fileToSignService;
	
	private final IFileSignedService fileSignedService;
	
	private final ICompanyFileService companyFileService;
	
	private final Converter converter;
	
	@GetMapping(value = "/filestosignregistry", produces = "application/json")
	public ResponseEntity<?> getFileToSignCompanyRegistry(){
		
		List<FileToSign> filesToSign = fileToSignService.findByCompanyFileTypeAndState(Long.valueOf(10), Long.valueOf(1));
		
		if (filesToSign.isEmpty()) {
			log.error("[FilesToSignRestController][getFileToSignCompanyRegistry][ms-loadfiles-neg]" + " No hay archivos de registro de empresa para firmar en la BD");
			throw new ResponseStatusException(HttpStatus.NO_CONTENT);
		}
		else {
			List<FileToSignDTOResponse> filesToSignDTO = filesToSign.stream().map(file -> converter.FileToSignToDTO(file))
					.collect(Collectors.toList());
			
			return ResponseEntity.status(HttpStatus.OK).body(filesToSignDTO);
		}
	}
	
	@PostMapping("/pdftosign")
	public ResponseEntity<byte[]> getPdfToSign(@Valid @RequestBody FileToSign file)  {
		Resource resource = null;
		File fileGetted = null;
		Optional<FileToSign> companyFileFounded = null;
		
		try {
			companyFileFounded = fileToSignService.findById(file.getId());
		} catch (Exception e) {
			log.error("[FilesToSignRestController][getPdfToSign][ms-loadfiles-neg]" + " Error al intentar obtener el archivo");
			throw new InternalServerErrorException("Error al intentar obtener el archivo");
		}
		
		try {
			resource = companyFileService.loadFile(companyFileFounded.get().getFileName(), ConstantVariables.FILES_REGISTRY_SIGN);
		} catch (MalformedURLException e) {
			log.error("[FilesToSignRestController][getPdfToSign][ms-loadfiles-neg]" + " Error al intentar obtener el archivo: " + companyFileFounded.get().getFileName());
			throw new InternalServerErrorException("Error al intentar obtener el archivo: " + companyFileFounded.get().getFileName());
		}
		
		try {
			fileGetted = resource.getFile();
		} catch (IOException e) {
			log.error("[FilesToSignRestController][getPdfToSign][ms-loadfiles-neg]" + " Error al intentar obtener el archivo del recurso: " + companyFileFounded.get().getFileName());
			throw new InternalServerErrorException("Error al intentar obtener el archivo del recurso: " + companyFileFounded.get().getFileName());
		}
		
		byte[] pdfBytes = null;
		
		try {
			pdfBytes = Files.readAllBytes(fileGetted.toPath());
		} catch (IOException e) {
			log.error("[FilesToSignRestController][getPdfToSign][ms-loadfiles-neg]" + " Error al intentar los bytes del archivo: " + companyFileFounded.get().getFileName());
			throw new InternalServerErrorException("Error al intentar los bytes del archivo: " + companyFileFounded.get().getFileName());
		}
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.setContentDisposition(ContentDisposition.builder("inline").filename(companyFileFounded.get().getFileName()).build());
		
		return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
	}
	
	@PostMapping("/uploadsignedpdf")
	public ResponseEntity<?> uploadSignedPdf(@RequestParam("file") MultipartFile file, 
											 @RequestParam("fileinfo") String jsoSignedFile){
		
		Map<String, Object> response = new HashMap<>();
		FileSignedDTORequest fileSignedDTO = new Gson().fromJson(jsoSignedFile, FileSignedDTORequest.class);
		
		if (file.isEmpty()) {
			log.error("[FilesToSignRestController][uploadSignedPdf][loadfiles]" + " No hay un archivo pdf firmado para subir");
			throw new BadRequestException("No hay un archivo pdf firmado para subir");
		}
		
		String pathFileSigned = ConstantVariables.FILES_REGISTRY_SIGNED + "/" + fileSignedDTO.getCompanyName();
		
		companyFileService.createFolder(pathFileSigned);
		
		String fileName = null;
		
		try {
			fileName = companyFileService.copyFile(file, pathFileSigned);
		} catch (Exception e) {
			log.error("[FilesToSignRestController][uploadSignedPdf][loadfiles]" + " Error al intentar subir el archivo pdf");
			throw new InternalServerErrorException("Error al intentar subir el archivo pdf");
		}
		
		FileSigned fileSigned = converter.FileSignedDTOtoFileSigned(fileSignedDTO);
		FileSigned newFileSigned = null;
		
		fileSigned.setFileName(fileName);
		fileSigned.setLoadTime(LocalDateTime.now());
		fileSigned.setFilePath(pathFileSigned);
		fileSigned.setState(Long.valueOf(1));
		
		try {
			newFileSigned = fileSignedService.save(fileSigned);
		} catch (Exception e) {
			log.error("[FilesToSignRestController][uploadSignedPdf][loadfiles]" + " Error al intentar guardar el registro del archivo en la base de datos: " + fileName);
			throw new InternalServerErrorException("Error al intentar guardar el registro del archivo en la base de datos: " + fileName);
		}
		
		response.put("saveFile", true);
		
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
	}

}
