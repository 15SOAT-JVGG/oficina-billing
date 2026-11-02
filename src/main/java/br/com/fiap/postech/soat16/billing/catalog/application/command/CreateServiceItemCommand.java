package br.com.fiap.postech.soat16.billing.catalog.application.command;

import java.math.BigDecimal;

public record CreateServiceItemCommand(
        String name,
        String description,
        BigDecimal basePrice,
        Integer estimatedDurationMinutes,
        Boolean active
) { }
