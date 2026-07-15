package com.loadfilesservice.loadfiles.web.controller;

import jakarta.validation.Valid;
import java.util.List;

import com.loadfilesservice.loadfiles.application.exception.ResourceNotFoundException;
import com.loadfilesservice.loadfiles.application.service.ISignDocumentTypeService;
import com.loadfilesservice.loadfiles.domain.SignDocumentType;
import com.loadfilesservice.loadfiles.web.dto.SignDocumentTypeDTORequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST del catálogo parametrizable de documentos a firmar (sign_document_type),
 * independiente del catálogo de documentos a cargar (CompanyFileType). Las operaciones de
 * administración requieren rol ADMINISTRADOR; el listado de vigentes es público.
 */
@Tag(name = "Catálogo de documentos a firmar", description = "Parametrización de los documentos que requieren firma")
@CrossOrigin(origins = {"http://localhost:4300", "http://localhost:4400", "http://localhost:4500",
        "http://localhost:4600", "http://localhost:4700"})
@RestController
@RequestMapping("/api/signdocumenttypeapi")
@RequiredArgsConstructor
@Slf4j
public class SignDocumentTypeRestController {

    private final ISignDocumentTypeService signDocumentTypeService;

    /** Lista los tipos de documento a firmar activos. */
    @Operation(summary = "Lista los tipos de documento a firmar activos", description = "Endpoint público.")
    @GetMapping(value = "/active", produces = "application/json")
    public ResponseEntity<List<SignDocumentType>> active() {
        return ResponseEntity.ok(signDocumentTypeService.findAllActive());
    }

    /** Lista todo el catálogo de documentos a firmar, incluyendo los inactivos. */
    @Operation(summary = "Lista todo el catálogo de documentos a firmar (incluye inactivos)",
            description = "Roles requeridos: ADMINISTRADOR.")
    @PreAuthorize("@rolValidator.hasRol(authentication, 'administracion')")
    @GetMapping(produces = "application/json")
    public ResponseEntity<List<SignDocumentType>> findAll() {
        return ResponseEntity.ok(signDocumentTypeService.findAll());
    }

    /** Crea un nuevo tipo de documento a firmar en el catálogo. */
    @Operation(summary = "Crea un nuevo tipo de documento a firmar", description = "Roles requeridos: ADMINISTRADOR.")
    @PreAuthorize("@rolValidator.hasRol(authentication, 'administracion')")
    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<SignDocumentType> create(@Valid @RequestBody SignDocumentTypeDTORequest request) {
        SignDocumentType signDocumentType = toEntity(new SignDocumentType(), request);
        SignDocumentType saved = signDocumentTypeService.save(signDocumentType);
        log.info("[SignDocumentTypeRestController][create][loadfiles] Documento a firmar creado: {}", saved.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /** Edita un tipo de documento a firmar existente del catálogo. */
    @Operation(summary = "Edita un tipo de documento a firmar existente", description = "Roles requeridos: ADMINISTRADOR.")
    @PreAuthorize("@rolValidator.hasRol(authentication, 'administracion')")
    @PutMapping(value = "/{id}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<SignDocumentType> update(
            @Parameter(description = "ID del tipo de documento a firmar") @PathVariable Long id,
            @Valid @RequestBody SignDocumentTypeDTORequest request) {
        SignDocumentType existing = signDocumentTypeService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("El tipo de documento a firmar no existe"));
        SignDocumentType updated = toEntity(existing, request);
        SignDocumentType saved = signDocumentTypeService.save(updated);
        return ResponseEntity.ok(saved);
    }

    /** Desactiva un tipo de documento a firmar (soft-delete). */
    @Operation(summary = "Desactiva un tipo de documento a firmar (soft-delete)", description = "Roles requeridos: ADMINISTRADOR.")
    @PreAuthorize("@rolValidator.hasRol(authentication, 'administracion')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(
            @Parameter(description = "ID del tipo de documento a firmar") @PathVariable Long id) {
        signDocumentTypeService.deactivate(id);
        log.info("[SignDocumentTypeRestController][deactivate][loadfiles] Documento a firmar {} desactivado", id);
        return ResponseEntity.noContent().build();
    }

    private SignDocumentType toEntity(SignDocumentType target, SignDocumentTypeDTORequest request) {
        target.setName(request.getName());
        target.setDescription(request.getDescription());
        target.setActivo(request.isActivo());
        return target;
    }

}
