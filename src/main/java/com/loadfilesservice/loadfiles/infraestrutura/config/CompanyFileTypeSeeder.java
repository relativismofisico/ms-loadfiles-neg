package com.loadfilesservice.loadfiles.infraestrutura.config;

import java.util.LinkedHashMap;
import java.util.Map;

import com.loadfilesservice.loadfiles.domain.CompanyFileType;
import com.loadfilesservice.loadfiles.infraestrutura.persistence.ICompanyFileTypeDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Siembra idempotente del catálogo parametrizable de documentos.
 * Los 9 documentos base ya existen en BD (ids 1-9, gestionados manualmente hasta ahora, con las
 * etiquetas que hasta ahora estaban hardcoded en el frontend) y solo se completan campos si aún
 * están vacíos. Los 4 documentos específicos por operación (factoring/confirming/fondeador) se
 * crean si no existen, buscados por nombre.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CompanyFileTypeSeeder {

    /** Etiquetas que hasta ahora estaban hardcoded en el template de ms-registroempresa-front-neg. */
    private static final Map<Long, String> BASE_DOCUMENT_LABELS = new LinkedHashMap<>();

    static {
        BASE_DOCUMENT_LABELS.put(1L, "Cámara y comercio");
        BASE_DOCUMENT_LABELS.put(2L, "RUT");
        BASE_DOCUMENT_LABELS.put(3L, "Composición accionaria");
        BASE_DOCUMENT_LABELS.put(4L, "EEFF");
        BASE_DOCUMENT_LABELS.put(5L, "Estados financieros");
        BASE_DOCUMENT_LABELS.put(6L, "Declaración de renta");
        BASE_DOCUMENT_LABELS.put(7L, "Resolución de facturación");
        BASE_DOCUMENT_LABELS.put(8L, "Estado de obligaciones con la DIAN");
        BASE_DOCUMENT_LABELS.put(9L, "Cédula del representante");
    }

    private final ICompanyFileTypeDao companyFileTypeDao;

    /** Ejecuta la siembra al arrancar la aplicación. */
    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        markBaseDocuments();
        seedOperationDocument("Ventas", "Reporte de ventas", true, true, false);
        seedOperationDocument("Proveedores", "Reporte de proveedores", false, true, false);
        seedOperationDocument("Ganancias", "Estado de ganancias", false, false, true);
        seedOperationDocument("Gran Contribuyente", "Certificado de gran contribuyente", false, false, true);
    }

    private void markBaseDocuments() {
        BASE_DOCUMENT_LABELS.forEach((id, label) -> companyFileTypeDao.findById(id).ifPresent(fileType -> {
            boolean needsUpdate = !Boolean.TRUE.equals(fileType.getEsBase())
                    || !Boolean.TRUE.equals(fileType.getActivo())
                    || isBlank(fileType.getDescription())
                    || isBlank(fileType.getFileTypeName());
            if (needsUpdate) {
                fileType.setEsBase(Boolean.TRUE);
                fileType.setActivo(Boolean.TRUE);
                if (isBlank(fileType.getDescription())) {
                    fileType.setDescription(label);
                }
                if (isBlank(fileType.getFileTypeName())) {
                    fileType.setFileTypeName(label);
                }
                companyFileTypeDao.save(fileType);
                log.info("[CompanyFileTypeSeeder][markBaseDocuments][loadfiles] "
                        + "Documento base id={} actualizado ({})", id, label);
            }
        }));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void seedOperationDocument(String name, String description,
            boolean factoring, boolean confirming, boolean fondeador) {
        if (companyFileTypeDao.findByFileTypeNameIgnoreCase(name).isPresent()) {
            return;
        }

        CompanyFileType fileType = new CompanyFileType();
        fileType.setFileTypeName(name);
        fileType.setDescription(description);
        fileType.setEsBase(Boolean.FALSE);
        fileType.setAplicaFactoring(factoring);
        fileType.setAplicaConfirming(confirming);
        fileType.setAplicaFondeador(fondeador);
        fileType.setActivo(Boolean.TRUE);
        companyFileTypeDao.save(fileType);
        log.info("[CompanyFileTypeSeeder][seedOperationDocument][loadfiles] Documento '{}' creado", name);
    }

}
