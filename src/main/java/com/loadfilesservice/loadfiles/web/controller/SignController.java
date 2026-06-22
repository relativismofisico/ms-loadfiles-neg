package com.loadfilesservice.loadfiles.web.controller;

import java.net.MalformedURLException;
import java.util.Optional;

import com.loadfilesservice.loadfiles.application.exception.InternalServerErrorException;
import com.loadfilesservice.loadfiles.application.service.IFileStorageService;
import com.loadfilesservice.loadfiles.application.service.ISignService;
import com.loadfilesservice.loadfiles.domain.Sign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Controlador REST para operaciones sobre firmas digitales. */
@CrossOrigin(origins = {"http://localhost:4300", "http://localhost:4400"})
@RestController
@RequestMapping("/signs")
@RequiredArgsConstructor
@Slf4j
public class SignController {

    private final ISignService signService;

    private final IFileStorageService fileStorageService;

    /** Guarda o reemplaza la firma activa de una empresa. */
    @PreAuthorize("@rolValidator.hasRol(authentication, 'firma-guardado')")
    @PostMapping("/save")
    public ResponseEntity<?> saveSign(
            @RequestParam("file") MultipartFile file,
            @RequestParam("company") Long company,
            @RequestParam("user") Long user,
            @RequestParam("ipLoad") String ipLoad,
            @RequestParam("companyName") String companyName) {

        Long effectiveUser;
        if (Long.valueOf(0L).equals(user)) {
            effectiveUser = null;
        } else {
            effectiveUser = user;
        }
        Sign savedSign = this.signService.replaceSign(file, company, effectiveUser, ipLoad, companyName);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSign);
    }

    /** Retorna el archivo de firma activa de una empresa. */
    @PreAuthorize("@rolValidator.hasRol(authentication, 'todos')")
    @GetMapping("/company/{companyId}")
    public ResponseEntity<Resource> getActiveSignByCompany(@PathVariable Long companyId) {

        Optional<Sign> signOptional;
        try {
            signOptional = this.signService.findActiveSignByCompany(companyId);
        } catch (Exception e) {
            log.error("[SignController][getActiveSignByCompany][loadfiles] Error al intentar obtener la firma en la BD", e);
            throw new InternalServerErrorException("Error al intentar obtener la firma en la BD", e);
        }

        if (signOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Sign sign = signOptional.get();
        try {
            Resource fileResource = this.fileStorageService.loadFile(sign.getFileName(), sign.getFilePath());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + sign.getFileName() + "\"")
                    .body(fileResource);
        } catch (MalformedURLException e) {
            log.error("[SignController][getActiveSignByCompany][loadfiles] Error al cargar la firma", e);
            throw new InternalServerErrorException("Error al cargar el archivo de firma: " + sign.getFileName());
        }
    }

    /** Retorna el archivo de firma activa de un usuario. */
    @PreAuthorize("@rolValidator.hasRol(authentication, 'todos')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<Resource> getActiveSignByUser(@PathVariable Long userId) {

        Optional<Sign> signOptional;
        try {
            signOptional = this.signService.findActiveSignByUser(userId);
        } catch (Exception e) {
            log.error("[SignController][getActiveSignByUser][loadfiles] Error al intentar obtener la firma en la BD", e);
            throw new InternalServerErrorException("Error al intentar obtener la firma en la BD", e);
        }

        if (signOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Sign sign = signOptional.get();
        try {
            Resource fileResource = this.fileStorageService.loadFile(sign.getFileName(), sign.getFilePath());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + sign.getFileName() + "\"")
                    .body(fileResource);
        } catch (MalformedURLException e) {
            log.error("[SignController][getActiveSignByUser][loadfiles] Error al cargar la firma", e);
            throw new InternalServerErrorException("Error al cargar el archivo de firma: " + sign.getFileName());
        }
    }

}
