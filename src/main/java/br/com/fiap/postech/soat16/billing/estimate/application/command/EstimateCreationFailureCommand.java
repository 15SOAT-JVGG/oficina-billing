package br.com.fiap.postech.soat16.billing.estimate.application.command;

import java.util.UUID;

public record EstimateCreationFailureCommand(UUID eventId, UUID workOrderId, UUID sagaId,
        UUID correlationId, UUID causationId, String code, String message) { }
