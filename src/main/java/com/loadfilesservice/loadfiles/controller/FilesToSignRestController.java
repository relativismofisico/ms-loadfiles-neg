package com.loadfilesservice.loadfiles.controller;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.gson.Gson;
import com.loadfilesservice.loadfiles.dto.Converter;
import com.loadfilesservice.loadfiles.dto.FileSignedDTORequest;
import com.loadfilesservice.loadfiles.dto.FileToSignDTOResponse;
import com.loadfilesservice.loadfiles.entity.FileSigned;
import com.loadfilesservice.loadfiles.entity.FileToSign;
import com.loadfilesservice.loadfiles.exceptions.BadRequestException;
import com.loadfilesservice.loadfiles.exceptions.InternalServerErrorException;
import com.loadfilesservice.loadfiles.exceptions.ResourceNotFoundException;
import com.loadfilesservice.loadfiles.service.IFileSignedService;
import com.loadfilesservice.loadfiles.service.IFileStorageService;
import com.loadfilesservice.loadfiles.service.IFileToSignService;
import com.loadfilesservice.loadfiles.util.ConstantVariables;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CrossOrigin(origins = { "http://localhost:4300", "http://localhost:4400" })
@RestController
@RequestMapping("/filesign")
@RequiredArgsConstructor
@Slf4j
public class FilesToSignRestController {

	private final IFileToSignService fileToSignService;

	private final IFileSignedService fileSignedService;

	private final IFileStorageService fileStorageService;

	private final Converter converter;

	@PreAuthorize("@rolValidator.hasRol(authentication, 'revision')")
	@GetMapping(value = "/filestosignregistry", produces = "application/json")
	public ResponseEntity<?> getFileToSignCompanyRegistry() {

		List<FileToSign> filesToSign = fileToSignService.findByCompanyFileTypeAndState(10L, 1L);

		if (filesToSign.isEmpty()) {
			log.error("[FilesToSignRestController][getFileToSignCompanyRegistry][ms-loadfiles-neg] No hay archivos de registro de empresa para firmar en la BD");
			throw new ResourceNotFoundException("No hay archivos de registro de empresa para firmar en la BD");
		} else {
			List<FileToSignDTOResponse> filesToSignDTO = filesToSign.stream()
					.map(converter::fileToSignToDTO)
					.toList();

			return ResponseEntity.status(HttpStatus.OK).body(filesToSignDTO);
		}
	}

	@PreAuthorize("@rolValidator.hasRol(authentication, 'revision')")
	@PostMapping("/pdftosign")
	public ResponseEntity<byte[]> getPdfToSign(@Valid @RequestBody FileToSign file) {
		Optional<FileToSign> companyFileFounded;
		try {
			companyFileFounded = fileToSignService.findById(file.getId());
		} catch (Exception e) {
			log.error("[FilesToSignRestController][getPdfToSign][ms-loadfiles-neg] Error al intentar obtener el archivo", e);
			throw new InternalServerErrorException("Error al intentar obtener el archivo", e);
		}

		FileToSign fileToSign = companyFileFounded
				.orElseThrow(() -> new InternalServerErrorException("Error al intentar obtener el archivo"));

		Resource resource;
		try {
			resource = fileStorageService.loadFile(fileToSign.getFileName(), ConstantVariables.FILES_REGISTRY_SIGN);
		} catch (MalformedURLException e) {
			log.error("[FilesToSignRestController][getPdfToSign][ms-loadfiles-neg] Error al intentar obtener el archivo: {}",
					fileToSign.getFileName());
			throw new InternalServerErrorException("Error al intentar obtener el archivo: " + fileToSign.getFileName());
		}

		File fileGetted;
		try {
			fileGetted = resource.getFile();
		} catch (IOException e) {
			log.error("[FilesToSignRestController][getPdfToSign][ms-loadfiles-neg] Error al intentar obtener el archivo del recurso: {}",
					fileToSign.getFileName(), e);
			throw new InternalServerErrorException("Error al intentar obtener el archivo del recurso: " + fileToSign.getFileName(), e);
		}

		byte[] pdfBytes;
		try {
			pdfBytes = Files.readAllBytes(fileGetted.toPath());
		} catch (IOException e) {
			log.error("[FilesToSignRestController][getPdfToSign][ms-loadfiles-neg] Error al intentar los bytes del archivo: {}",
					fileToSign.getFileName(), e);
			throw new InternalServerErrorException("Error al intentar los bytes del archivo: " + fileToSign.getFileName(), e);
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.setContentDisposition(ContentDisposition.builder("inline").filename(fileToSign.getFileName()).build());

		return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
	}

	@PreAuthorize("@rolValidator.hasRol(authentication, 'firma-subida')")
	@PostMapping("/uploadsignedpdf")
	public ResponseEntity<?> uploadSignedPdf(@RequestParam("file") MultipartFile file,
											 @RequestParam("fileinfo") String jsoSignedFile) {

		Map<String, Object> response = new HashMap<>();
		FileSignedDTORequest fileSignedDTO = new Gson().fromJson(jsoSignedFile, FileSignedDTORequest.class);

		if (file.isEmpty()) {
			log.error("[FilesToSignRestController][uploadSignedPdf][loadfiles] No hay un archivo pdf firmado para subir");
			throw new BadRequestException("No hay un archivo pdf firmado para subir");
		}

		FileSigned fileSignedBase = converter.fileSignedDtoToFileSigned(fileSignedDTO);
		fileSignedService.saveSignedFile(file, fileSignedBase, fileSignedDTO.getCompanyName());

		response.put("saveFile", true);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

}