package br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import br.com.fiap.postech.soat16.billing.catalog.domain.model.PartType;

/**
 * Sem {@code stockQuantity}/{@code minimumStock}: o saldo é do Execution. Clientes que ainda os
 * enviam continuam atendidos, porque propriedades desconhecidas são ignoradas.
 */
public record PartRequestDto(
        @NotBlank(message = "Part name is required") String name,
        String description,
        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than zero") BigDecimal unitPrice,
        @NotBlank(message = "Unit is required") String unit,
        PartType partType
) { }
