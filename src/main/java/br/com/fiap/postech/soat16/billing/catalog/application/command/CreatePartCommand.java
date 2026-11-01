package br.com.fiap.postech.soat16.billing.catalog.application.command;

import java.math.BigDecimal;

import br.com.fiap.postech.soat16.billing.catalog.domain.model.PartType;

public record CreatePartCommand(
        String name,
        String description,
        BigDecimal unitPrice,
        String unit,
        PartType partType
) { }
