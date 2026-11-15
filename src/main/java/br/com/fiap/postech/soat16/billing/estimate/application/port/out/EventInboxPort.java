package br.com.fiap.postech.soat16.billing.estimate.application.port.out;

import java.util.UUID;

import io.smallrye.mutiny.Uni;

@FunctionalInterface
public interface EventInboxPort {
    /** Retorna false quando o eventId já foi confirmado por este consumidor. */
    Uni<Boolean> claim(UUID eventId, UUID workOrderId, String eventType);
}
