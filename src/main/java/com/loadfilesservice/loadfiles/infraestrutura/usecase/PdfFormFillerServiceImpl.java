package com.loadfilesservice.loadfiles.infraestrutura.usecase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import com.loadfilesservice.loadfiles.application.exception.InternalServerErrorException;
import com.loadfilesservice.loadfiles.application.service.IPdfFormFillerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.springframework.stereotype.Service;

/** Implementación del servicio de relleno de formularios PDF (AcroForm) usando Apache PDFBox. */
@Service
@Slf4j
public class PdfFormFillerServiceImpl implements IPdfFormFillerService {

    @Override
    public byte[] fill(byte[] templateBytes, Map<String, String> fieldValues) {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(templateBytes))) {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();

            if (acroForm != null) {
                for (Map.Entry<String, String> entry : fieldValues.entrySet()) {
                    PDField field = acroForm.getField(entry.getKey());
                    if (field != null) {
                        field.setValue(entry.getValue());
                    }
                }
                acroForm.setNeedAppearances(false);
            }

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                document.save(out);
                return out.toByteArray();
            }
        } catch (IOException e) {
            log.error("[PdfFormFillerServiceImpl][fill][loadfiles] Error al intentar rellenar el formulario PDF", e);
            throw new InternalServerErrorException("Error al intentar rellenar el formulario PDF", e);
        }
    }

}
