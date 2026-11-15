package br.com.fiap.postech.soat16.billing.catalog.application.result;

import java.math.BigDecimal;
import java.util.UUID;

/** Campos do catálogo necessários a um orçamento; não inclui dados de estoque. */
public record CatalogSnapshotResult(UUID id, String name, BigDecimal unitPrice, boolean active) { }
