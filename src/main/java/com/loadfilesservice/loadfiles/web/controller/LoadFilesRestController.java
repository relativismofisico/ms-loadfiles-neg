package com.loadfilesservice.loadfiles.web.controller;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loadfilesservice.loadfiles.application.ConstantVariables;
import com.loadfilesservice.loadfiles.application.exception.BadRequestException;
import com.loadfilesservice.loadfiles.application.exception.InternalServerErrorException;
import com.loadfilesservice.loadfiles.application.exception.ResourceNotFoundException;
import com.loadfilesservice.loadfiles.application.service.ICompanyFileService;
import com.loadfilesservice.loadfiles.application.service.IFileStorageService;
import com.loadfilesservice.loadfiles.domain.CompanyFile;
import com.loadfilesservice.loadfiles.domain.CompanyFileType;
import com.loadfilesservice.loadfiles.web.dto.CompanyFileDTORequest;
import com.loadfilesservice.loadfiles.web.dto.CompanyFileDTOResponse;
import com.loadfilesservice.loadfiles.web.dto.Converter;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CrossOrigin(origins = {"http://localhost:4300", "http://localhost:4600"})
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class LoadFilesRestController {

    private final ICompanyFileService companyFileService;

    private final IFileStorageService fileStorageService;

    private final Converter converter;

    private final ObjectMapper objectMapper;

    @PreAuthorize("@rolValidator.hasRol(authentication, 'carga')")
    @PostMapping(value = "/companyfile/upload", produces = "application/json")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile newfile,
                                        @RequestParam("fileinfo") String jsonCompanyFile) {

        Map<String, Object> response = new HashMap<>();
        CompanyFileDTORequest companyFileDTO;
        try {
            companyFileDTO = objectMapper.readValue(jsonCompanyFile, CompanyFileDTORequest.class);
        } catch (JsonProcessingException e) {
            log.error("[LoadFilesRestController][uploadFile][loadfiles] Error al parsear el JSON del archivo", e);
            throw new BadRequestException("El formato del JSON de información del archivo es inválido");
        }

        if (!newfile.isEmpty()) {
            CompanyFile companyFileBase = converter.companyFileDTOtoCompanyFile(companyFileDTO);
            CompanyFile savedFile = companyFileService.replaceCompanyFile(newfile, companyFileBase);

            log.info("[LoadFilesRestController][uploadFile][loadfiles] Archivo guardado: {}", savedFile);

            response.put("savedFile", savedFile);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("@rolValidator.hasRol(authentication, 'todos')")
    @PostMapping("/companyfile/upload/file/{id}")
    public ResponseEntity<?> getFile(@Valid @RequestBody CompanyFileType fileType, @PathVariable Long id) {

        Map<String, Object> response = new HashMap<>();

        List<CompanyFile> listCompanyFile;
        try {
            listCompanyFile = companyFileService.findByCompanyAndCompanyFileType(id, fileType);
        } catch (Exception e) {
            log.error("[LoadFilesRestController][getFile][loadfiles] Error al intentar obtener el archivo", e);
            throw new InternalServerErrorException("Error al intentar obtener el archivo", e);
        }

        listCompanyFile.stream()
                .filter(cf -> Long.valueOf(1L).equals(cf.getState()))
                .findFirst()
                .ifPresent(cf -> response.put("fileName", cf.getOriginalFileName()));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PreAuthorize("@rolValidator.hasRol(authentication, 'todos')")
    @GetMapping(value = "/listofcompanyfiles/{id}", produces = "application/json")
    public ResponseEntity<?> getListOfCompanyFiles(@PathVariable Long id) {
        List<CompanyFile> companyFiles = companyFileService.findByCompanyAndState(id, 1L);

        if (companyFiles.isEmpty()) {
            log.error("[LoadFilesRestController][getListOfCompanyFiles][loadfiles] No hay archivos de la empresa en la BD");
            throw new ResourceNotFoundException("No hay archivos de la empresa en la BD");
        } else {
            List<CompanyFileDTOResponse> companyFilesDTO = companyFiles.stream()
                    .map(converter::companyFileToDTO)
                    .toList();

            return ResponseEntity.status(HttpStatus.OK).body(companyFilesDTO);
        }
    }

    @PreAuthorize("@rolValidator.hasRol(authentication, 'todos')")
    @PostMapping("/companyfilepdf")
    public ResponseEntity<byte[]> getPdfCompanyFile(@Valid @RequestBody CompanyFile file) {
        Optional<CompanyFile> companyFileFoundedOpt;
        try {
            companyFileFoundedOpt = companyFileService.findById(file.getId());
        } catch (Exception e) {
            log.error("[LoadFilesRestController][getPdfCompanyFile][ms-loadfiles-neg] Error al intentar obtener el archivo", e);
            throw new InternalServerErrorException("Error al intentar obtener el archivo", e);
        }

        CompanyFile companyFileFounded = companyFileFoundedOpt
                .orElseThrow(() -> new InternalServerErrorException("Error al intentar obtener el archivo"));

        Resource resource;
        try {
            resource = fileStorageService.loadFile(companyFileFounded.getFileName(), ConstantVariables.PATH_UPLOADS);
        } catch (MalformedURLException e) {
            log.error("[LoadFilesRestController][getPdfCompanyFile][ms-loadfiles-neg] Error al intentar obtener el archivo: {}",
                    companyFileFounded.getFileName());
            throw new InternalServerErrorException("Error al intentar obtener el archivo: " + companyFileFounded.getFileName());
        }

        File fileGetted;
        try {
            fileGetted = resource.getFile();
        } catch (IOException e) {
            log.error("[LoadFilesRestController][getPdfCompanyFile][ms-loadfiles-neg]"
                    + " Error al intentar obtener el archivo del recurso: {}",
                    companyFileFounded.getFileName(), e);
            throw new InternalServerErrorException(
                    "Error al intentar obtener el archivo del recurso: "
                            + companyFileFounded.getFileName(), e);
        }

        byte[] pdfBytes;
        try {
            pdfBytes = Files.readAllBytes(fileGetted.toPath());
        } catch (IOException e) {
            log.error("[LoadFilesRestController][getPdfCompanyFile][ms-loadfiles-neg] Error al intentar los bytes del archivo: {}",
                    companyFileFounded.getFileName(), e);
            throw new InternalServerErrorException("Error al intentar los bytes del archivo: " + companyFileFounded.getFileName(), e);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.builder("inline").filename(companyFileFounded.getFileName()).build());

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

}
