package com.loadfilesservice.loadfiles.controller;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

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

import com.loadfilesservice.loadfiles.dto.Converter;
import com.loadfilesservice.loadfiles.entity.FileSigned;
import com.loadfilesservice.loadfiles.exceptions.InternalServerErrorException;
import com.loadfilesservice.loadfiles.exceptions.ResourceNotFoundException;
import com.loadfilesservice.loadfiles.service.ICompanyFileService;
import com.loadfilesservice.loadfiles.service.IFileSignedService;
import com.loadfilesservice.loadfiles.util.ConstantVariables;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CrossOrigin(origins = { "http://localhost:4300", "http://localhost:4600" })
@RestController
@RequestMapping("/filessigned")
@RequiredArgsConstructor
@Slf4j
public class FilesSignedRestController {
	
	private final IFileSignedService fileSignedService;
	
	private final ICompanyFileService companyFileService;
	
	private final Converter converter;
	
	@GetMapping(value = "/listofcompanysignedfiles/{id}", produces = "application/json")
	public ResponseEntity<?> getListOfCompanySignedFiles(@PathVariable Long id){
		List<FileSigned> filesSigned = fileSignedService.findByCompanyAndState(id, Long.valueOf(1));
		
		if (filesSigned.isEmpty()) {
			log.error("[FilesToSignRestController][getListOfCompanySignedFiles][loadfiles]" + " No hay archivos firmados de la empresa en la BD");
			throw new ResourceNotFoundException("No hay archivos firmados de la empresa en la BD");
		}
		else {
			return ResponseEntity.status(HttpStatus.OK).body(filesSigned);
		}
	}
	
	@PostMapping("/companyfilesignedpdf/{companyName}")
	public ResponseEntity<byte[]> getPdfCompanyFileSigned(@Valid @RequestBody FileSigned file, @PathVariable String companyName)  {
		Resource resource = null;
		File fileGetted = null;
		Optional<FileSigned> companyFileSignedFounded = null;
		
		try {
			companyFileSignedFounded = fileSignedService.findById(file.getId());
		} catch (Exception e) {
			log.error("[FilesSignedRestController][getPdfCompanyFileSigned][ms-loadfiles-neg]" + " Error al intentar obtener el archivo");
			throw new InternalServerErrorException("Error al intentar obtener el archivo");
		}
		
		try {
			resource = companyFileService.loadFile(companyFileSignedFounded.get().getFileName(), ConstantVariables.FILES_REGISTRY_SIGNED + "/" + companyName);
		} catch (MalformedURLException e) {
			log.error("[FilesSignedRestController][getPdfCompanyFileSigned][ms-loadfiles-neg]" + " Error al intentar obtener el archivo: " + companyFileSignedFounded.get().getFileName());
			throw new InternalServerErrorException("Error al intentar obtener el archivo: " + companyFileSignedFounded.get().getFileName());
		}
		
		try {
			fileGetted = resource.getFile();
		} catch (IOException e) {
			log.error("[FilesSignedRestController][getPdfCompanyFileSigned][ms-loadfiles-neg]" + " Error al intentar obtener el archivo del recurso: " + companyFileSignedFounded.get().getFileName());
			throw new InternalServerErrorException("Error al intentar obtener el archivo del recurso: " + companyFileSignedFounded.get().getFileName());
		}
		
		byte[] pdfBytes = null;
		
		try {
			pdfBytes = Files.readAllBytes(fileGetted.toPath());
		} catch (IOException e) {
			log.error("[FilesSignedRestController][getPdfCompanyFileSigned][ms-loadfiles-neg]" + " Error al intentar pasar a bytes del archivo: " + companyFileSignedFounded.get().getFileName());
			throw new InternalServerErrorException("Error al intentar pasar a bytes del archivo: " + companyFileSignedFounded.get().getFileName());
		}
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.setContentDisposition(ContentDisposition.builder("inline").filename(companyFileSignedFounded.get().getFileName()).build());
		
		return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
		
	}

}
