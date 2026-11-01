package br.com.fiap.postech.soat16.billing.catalog.application.command;

import java.math.BigDecimal;
import java.util.UUID;

import br.com.fiap.postech.soat16.billing.catalog.domain.model.PartType;

public record UpdatePartCommand(
        UUID id,
        String name,
        String description,
        BigDecimal unitPrice,
        String unit,
        PartType partType
) { }
