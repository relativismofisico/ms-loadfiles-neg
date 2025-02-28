package com.loadfilesservice.loadfiles.controller;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.util.List;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.loadfilesservice.loadfiles.dto.Converter;
import com.loadfilesservice.loadfiles.dto.FileToSignDTOResponse;
import com.loadfilesservice.loadfiles.entity.FileToSign;
import com.loadfilesservice.loadfiles.exceptions.InternalServerErrorException;
import com.loadfilesservice.loadfiles.service.ICompanyFileService;
import com.loadfilesservice.loadfiles.service.IFileToSignService;
import com.loadfilesservice.loadfiles.util.ConstantVariables;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CrossOrigin(origins = { "http://localhost:4200" })
@RestController
@RequestMapping("/filesign")
@RequiredArgsConstructor
@Slf4j
public class FilesToSignRestController {
	
	private final IFileToSignService fileToSignService;
	
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

}
