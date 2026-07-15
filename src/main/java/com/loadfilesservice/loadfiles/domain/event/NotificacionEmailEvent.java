package com.loadfilesservice.loadfiles.domain.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Evento de solicitud de envío de correo, publicado hacia ms-notificaciones-util. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionEmailEvent {

    private String idEvento;
    private String idNegociacion;
    private String tipoNotificacion;
    private List<DestinatarioEmail> destinatarios;
    private String asunto;
    private Map<String, Object> data;
    private LocalDateTime fecha;
}
