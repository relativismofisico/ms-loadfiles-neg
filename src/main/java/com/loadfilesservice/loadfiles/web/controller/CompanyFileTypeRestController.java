package com.loadfilesservice.loadfiles.web.controller;

import jakarta.validation.Valid;
import java.util.List;

import com.loadfilesservice.loadfiles.application.exception.ResourceNotFoundException;
import com.loadfilesservice.loadfiles.application.service.ICompanyFileTypeService;
import com.loadfilesservice.loadfiles.domain.CompanyFileType;
import com.loadfilesservice.loadfiles.web.dto.CompanyFileTypeDTORequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST del catálogo parametrizable de tipos de documento (documentos_parametrizacion).
 * Las operaciones de administración (crear/editar/desactivar/listar todos) requieren rol ADMINISTRADOR.
 * La consulta de documentos aplicables es pública, usada por el flujo anónimo de registro de empresa.
 */
@Tag(name = "Catálogo de documentos", description = "Parametrización de los tipos de documento requeridos por operación")
@CrossOrigin(origins = {"http://localhost:4300", "http://localhost:4400", "http://localhost:4500",
        "http://localhost:4600", "http://localhost:4700"})
@RestController
@RequestMapping("/api/companyfiletypeapi")
@RequiredArgsConstructor
@Slf4j
public class CompanyFileTypeRestController {

    private final ICompanyFileTypeService companyFileTypeService;

    /** Lista los documentos aplicables según las operaciones de interés seleccionadas. */
    @Operation(summary = "Lista los documentos aplicables según las operaciones de interés seleccionadas",
            description = "Endpoint público, usado por el formulario de registro de empresa. Si no se marca "
                    + "ninguna operación, retorna el catálogo completo (todos los documentos activos).")
    @GetMapping(value = "/applicable", produces = "application/json")
    public ResponseEntity<List<CompanyFileType>> applicable(
            @Parameter(description = "Interesada en factoring") @RequestParam(defaultValue = "false") boolean factoring,
            @Parameter(description = "Interesada en confirming") @RequestParam(defaultValue = "false") boolean confirming,
            @Parameter(description = "Interesada en ser fondeador") @RequestParam(defaultValue = "false") boolean fondeador) {

        boolean ninguna = !factoring && !confirming && !fondeador;
        List<CompanyFileType> result;
        if (ninguna) {
            result = companyFileTypeService.findAllActive();
        } else {
            result = companyFileTypeService.findApplicable(factoring, confirming, fondeador);
        }

        return ResponseEntity.ok(result);
    }

    /** Lista todo el catálogo de documentos, incluyendo los inactivos. */
    @Operation(summary = "Lista todo el catálogo de documentos (incluye inactivos)",
            description = "Roles requeridos: ADMINISTRADOR.")
    @PreAuthorize("@rolValidator.hasRol(authentication, 'administracion')")
    @GetMapping(produces = "application/json")
    public ResponseEntity<List<CompanyFileType>> findAll() {
        return ResponseEntity.ok(companyFileTypeService.findAll());
    }

    /** Crea un nuevo tipo de documento en el catálogo. */
    @Operation(summary = "Crea un nuevo tipo de documento en el catálogo",
            description = "Roles requeridos: ADMINISTRADOR.")
    @PreAuthorize("@rolValidator.hasRol(authentication, 'administracion')")
    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<CompanyFileType> create(@Valid @RequestBody CompanyFileTypeDTORequest request) {
        CompanyFileType companyFileType = toEntity(new CompanyFileType(), request);
        CompanyFileType saved = companyFileTypeService.save(companyFileType);
        log.info("[CompanyFileTypeRestController][create][loadfiles] Documento creado: {}", saved.getFileTypeName());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /** Edita un tipo de documento existente del catálogo. */
    @Operation(summary = "Edita un tipo de documento existente del catálogo",
            description = "Roles requeridos: ADMINISTRADOR.")
    @PreAuthorize("@rolValidator.hasRol(authentication, 'administracion')")
    @PutMapping(value = "/{id}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<CompanyFileType> update(
            @Parameter(description = "ID del tipo de documento") @PathVariable Long id,
            @Valid @RequestBody CompanyFileTypeDTORequest request) {
        CompanyFileType existing = companyFileTypeService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("El tipo de documento no existe"));
        CompanyFileType updated = toEntity(existing, request);
        CompanyFileType saved = companyFileTypeService.save(updated);
        return ResponseEntity.ok(saved);
    }

    /** Desactiva un tipo de documento (soft-delete). */
    @Operation(summary = "Desactiva un tipo de documento (soft-delete)",
            description = "Roles requeridos: ADMINISTRADOR.")
    @PreAuthorize("@rolValidator.hasRol(authentication, 'administracion')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(
            @Parameter(description = "ID del tipo de documento") @PathVariable Long id) {
        companyFileTypeService.deactivate(id);
        log.info("[CompanyFileTypeRestController][deactivate][loadfiles] Documento {} desactivado", id);
        return ResponseEntity.noContent().build();
    }

    private CompanyFileType toEntity(CompanyFileType target, CompanyFileTypeDTORequest request) {
        target.setDescription(request.getDescription());
        target.setFileTypeName(request.getFileTypeName());
        target.setEsBase(request.isEsBase());
        target.setAplicaFactoring(request.isAplicaFactoring());
        target.setAplicaConfirming(request.isAplicaConfirming());
        target.setAplicaFondeador(request.isAplicaFondeador());
        target.setActivo(request.isActivo());
        return target;
    }

}
