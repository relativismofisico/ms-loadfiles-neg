package com.loadfilesservice.loadfiles.application.service;

import java.util.Map;

/** Contrato del servicio de relleno de formularios PDF (AcroForm). */
public interface IPdfFormFillerService {

    /** Rellena los campos de formulario del PDF con los valores dados y retorna los bytes resultantes. */
    byte[] fill(byte[] templateBytes, Map<String, String> fieldValues);

}
