package br.com.fiap.postech.soat16.billing.estimate.application.result;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogItemSnapshotResult(UUID id, String name, BigDecimal unitPrice, boolean active) { }
