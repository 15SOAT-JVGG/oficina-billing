package br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ServiceItemRequestDto(
        @NotBlank(message = "Service name is required") String name,
        String description,
        @NotNull(message = "Base price is required")
        @DecimalMin(value = "0.01", message = "Base price must be greater than zero") BigDecimal basePrice,
        @Min(value = 0, message = "Estimated duration cannot be negative") Integer estimatedDurationMinutes,
        Boolean active
) { }
