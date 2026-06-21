package com.loadfilesservice.loadfiles.controller;

import java.io.IOException;
import java.net.MalformedURLException;
import java.time.LocalDateTime;
import java.util.Optional;

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

import com.loadfilesservice.loadfiles.entity.Sign;
import com.loadfilesservice.loadfiles.exceptions.InternalServerErrorException;
import com.loadfilesservice.loadfiles.service.ICompanyFileService;
import com.loadfilesservice.loadfiles.service.ISignService;
import com.loadfilesservice.loadfiles.util.ConstantVariables;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CrossOrigin(origins = { "http://localhost:4300", "http://localhost:4400" })
@RestController
@RequestMapping("/signs")
@RequiredArgsConstructor
@Slf4j
public class SignController {
	
	private final ISignService signService;
    private final ICompanyFileService companyFileService;
    
    
    /**
     * Guarda una firma en el servidor y en la base de datos.
     */
    @PreAuthorize("@rolValidator.hasRol(authentication, 'firma-guardado')")
    @PostMapping("/save")
    public ResponseEntity<?> saveSign(
            @RequestParam("file") MultipartFile file,
            @RequestParam("company") Long company,
            @RequestParam("user") Long user,
            @RequestParam("ipLoad") String ipLoad,
            @RequestParam("companyName") String companyName){
    	
    	if (user == 0) {
			user = null;
		}
    	
    	String pathFileSign = ConstantVariables.SIGNATURES_FOLDER + "/" + companyName;
		companyFileService.createFolder(pathFileSign);
		
		Optional<Sign> existingSign = null;
		
		// Verificar si ya existe una firma activa para la empresa
		try {
			 existingSign = signService.findActiveSignByCompany(company);
		} catch (Exception e) {
			log.error("[SignController][saveSign][loadfiles]" + " Error al intentar consultar la firma en la base de datos");
			throw new InternalServerErrorException("Error al intentar consultar la firma en la base de datos " + e.getMessage());
		}
    	
		if (existingSign.isPresent()) {
            Sign oldSign = existingSign.get();
            
            // Eliminar el archivo antiguo antes de marcarlo como inactivo
            boolean deleted = companyFileService.deleteFile(oldSign.getFileName(), pathFileSign);
            if (deleted) {
                log.info("Archivo antiguo eliminado: " + oldSign.getFilePath() + "/" + oldSign.getFileName());
            } else {
                log.warn("No se pudo eliminar el archivo antiguo: " + oldSign.getFilePath() + "/" + oldSign.getFileName());
            }

            oldSign.setState(0L); // Marcar como inactiva
            
            try {
            	signService.save(oldSign);
			} catch (Exception e) {
				log.error("[SignController][saveSign][loadfiles]" + " Error al intentar actualizar la firma antigua en la BD");
				throw new InternalServerErrorException("Error al intentar actualizar la firma antigua en la BD " + e.getMessage());
			}
        }
		
		String fileName = null;
		
		// Guardar el nuevo archivo en la ruta especificada
        try {
			fileName = companyFileService.copyFile(file, pathFileSign);
		} catch (IOException e) {
			log.error("[SignController][saveSign][loadfiles]" + " Error al intentar borrar la firma antigua de la ruta");
			throw new InternalServerErrorException("Error al intentar borrar la firma antigua de la ruta " + e.getMessage());
		}
        
     // Guardar la nueva firma en la base de datos
        Sign sign = new Sign();
        sign.setFileName(fileName);
        sign.setFilePath(pathFileSign);
        sign.setIpLoad(ipLoad);
        sign.setCompany(company);
        sign.setUser(user);
        sign.setState(1L); // Estado activo
        sign.setLoadTime(LocalDateTime.now());

        Sign savedSign = null;
        
        try {
        	savedSign = signService.save(sign);
		} catch (Exception e) {
			log.error("[SignController][saveSign][loadfiles]" + " Error al intentar guardar la nueva firma en la BD");
			throw new InternalServerErrorException("Error al intentar guardar la nueva firma en la BD " + e.getMessage());
		}
        
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSign);
    }
    
    /**
     * Obtiene la firma activa de una empresa.
     */
    @PreAuthorize("@rolValidator.hasRol(authentication, 'todos')")
    @GetMapping("/company/{companyId}")
    public ResponseEntity<Resource> getActiveSignByCompany(@PathVariable Long companyId) {
    	
    	Optional<Sign> signOptional = null;
    	
    	try {
    		signOptional = signService.findActiveSignByCompany(companyId);
		} catch (Exception e) {
			log.error("[SignController][getActiveSignByCompany][loadfiles]" + " Error al intentar obtener la firma en la BD");
			throw new InternalServerErrorException("Error al intentar obtener la firma en la BD " + e.getMessage());
		}
        
        if (signOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Sign sign = signOptional.get();
        try {
            Resource file = companyFileService.loadFile(sign.getFileName(), sign.getFilePath());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + sign.getFileName() + "\"")
                    .body(file);
        } catch (MalformedURLException e) {
            log.error("[SignController][getActiveSignByCompany][loadfiles] Error al cargar la firma", e);
            throw new InternalServerErrorException("Error al cargar el archivo de firma: " + sign.getFileName());
        }
    }

    /**
     * Obtiene la firma activa de un usuario.
     */
    @PreAuthorize("@rolValidator.hasRol(authentication, 'todos')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<Resource> getActiveSignByUser(@PathVariable Long userId) {

    	Optional<Sign> signOptional = null;

    	try {
    		signOptional = signService.findActiveSignByUser(userId);
		} catch (Exception e) {
			log.error("[SignController][getActiveSignByUser][loadfiles]" + " Error al intentar obtener la firma en la BD");
			throw new InternalServerErrorException("Error al intentar obtener la firma en la BD " + e.getMessage());
		}

        if (signOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Sign sign = signOptional.get();
        try {
            Resource file = companyFileService.loadFile(sign.getFileName(), sign.getFilePath());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + sign.getFileName() + "\"")
                    .body(file);
        } catch (MalformedURLException e) {
            log.error("[SignController][getActiveSignByUser][loadfiles] Error al cargar la firma", e);
            throw new InternalServerErrorException("Error al cargar el archivo de firma: " + sign.getFileName());
        }
    }

}
