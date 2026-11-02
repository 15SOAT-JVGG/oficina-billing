package br.com.fiap.postech.soat16.billing.catalog.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import br.com.fiap.postech.soat16.billing.catalog.domain.exception.InvalidServiceItemException;
import br.com.fiap.postech.soat16.billing.shared.domain.model.audit.AuditableEntity;

import lombok.Getter;

/**
 * Serviço do catálogo: mão de obra oferecida pela oficina, com preço base e duração estimada.
 * Os invariantes (nome, preço positivo, duração não negativa) são validados aqui, não só na borda.
 */
@Getter
public class ServiceItem extends AuditableEntity {

    private final UUID id;

    private String name;

    private String description;

    private BigDecimal basePrice;

    private Integer estimatedDurationMinutes;

    private boolean active;

    private ServiceItem(
            UUID id,
            String name,
            String description,
            BigDecimal basePrice,
            Integer estimatedDurationMinutes,
            boolean active,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String createdBy,
            String updatedBy) {
        super(createdAt, updatedAt, createdBy, updatedBy);
        this.id = id;
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.active = active;
    }

    /**
     * Registra um serviço novo no catálogo. {@code active} ausente significa ativo.
     */
    public static ServiceItem register(
            String name,
            String description,
            BigDecimal basePrice,
            Integer estimatedDurationMinutes,
            Boolean active) {
        validate(name, basePrice, estimatedDurationMinutes);
        return new ServiceItem(
                null, name, description, basePrice, estimatedDurationMinutes, activeOrDefault(active),
                null, null, null, null);
    }

    /**
     * Reidrata um serviço já persistido, preservando identidade e auditoria; não revalida.
     */
    public static ServiceItem restore(
            UUID id,
            String name,
            String description,
            BigDecimal basePrice,
            Integer estimatedDurationMinutes,
            boolean active,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String createdBy,
            String updatedBy) {
        return new ServiceItem(
                id, name, description, basePrice, estimatedDurationMinutes, active,
                createdAt, updatedAt, createdBy, updatedBy);
    }

    /**
     * Substitui todos os dados do serviço (PUT é substituição completa).
     */
    public void updateDetails(
            String name,
            String description,
            BigDecimal basePrice,
            Integer estimatedDurationMinutes,
            Boolean active) {
        validate(name, basePrice, estimatedDurationMinutes);
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.active = activeOrDefault(active);
    }

    private static void validate(String name, BigDecimal basePrice, Integer estimatedDurationMinutes) {
        if (name == null || name.isBlank()) {
            throw new InvalidServiceItemException("Service name is required");
        }
        if (basePrice == null) {
            throw new InvalidServiceItemException("Base price is required");
        }
        if (basePrice.signum() <= 0) {
            throw new InvalidServiceItemException("Base price must be greater than zero");
        }
        if (estimatedDurationMinutes != null && estimatedDurationMinutes < 0) {
            throw new InvalidServiceItemException("Estimated duration cannot be negative");
        }
    }

    private static boolean activeOrDefault(Boolean active) {
        return active == null || active;
    }

    /**
     * Identidade só existe após a persistência: duas instâncias ainda sem id nunca são iguais.
     */
    @Override
    public final boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ServiceItem that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    /**
     * Constante por classe porque o id muda de null para um valor na persistência; um hash baseado
     * nele quebraria coleções que recebessem a instância antes de salvar.
     */
    @Override
    public final int hashCode() {
        return ServiceItem.class.hashCode();
    }
}
