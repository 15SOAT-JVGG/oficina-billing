package br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import br.com.fiap.postech.soat16.billing.catalog.domain.model.PartType;

public record PartResponseDto(
        UUID id,
        String name,
        String description,
        BigDecimal unitPrice,
        String unit,
        PartType partType,
        OffsetDateTime createdAt
) { }
