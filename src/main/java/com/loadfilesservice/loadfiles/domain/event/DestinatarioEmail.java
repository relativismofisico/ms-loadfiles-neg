package com.loadfilesservice.loadfiles.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Destinatario de un evento de notificación por correo. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DestinatarioEmail {

    private String rolActor;
    private String rutActor;
    private String email;
    private String tipoActor;
}
