package br.com.fiap.postech.soat16.billing.catalog.application.command;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateServiceItemCommand(
        UUID id,
        String name,
        String description,
        BigDecimal basePrice,
        Integer estimatedDurationMinutes,
        Boolean active
) { }
