package com.loadfilesservice.loadfiles.infraestrutura.kafka;

import com.loadfilesservice.loadfiles.domain.event.NotificacionEmailEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/** Productor de mensajes Kafka del microservicio. */
@Service
@RequiredArgsConstructor
public class KafkaProducerAdapter {

    private static final String TOPIC_NOTIFICACION_EMAIL = "notificacion.email";

    private final KafkaTemplate<String, Object> jsonKafkaTemplate;

    /** Publica un evento de notificación por correo hacia ms-notificaciones-util. */
    public void sendNotificacionEmail(NotificacionEmailEvent event) {
        jsonKafkaTemplate.send(TOPIC_NOTIFICACION_EMAIL, event);
    }
}
