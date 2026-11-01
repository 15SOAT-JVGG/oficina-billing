package br.com.fiap.postech.soat16.billing.catalog.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import br.com.fiap.postech.soat16.billing.catalog.domain.exception.InvalidPartException;
import br.com.fiap.postech.soat16.billing.shared.domain.model.audit.AuditableEntity;

import lombok.Getter;

/**
 * Peça do catálogo: item físico com preço unitário e unidade de medida, classificado como peça ou
 * insumo. O saldo de estoque não é deste serviço (é do Execution), por isso não há quantidade aqui.
 */
@Getter
public class Part extends AuditableEntity {

    private final UUID id;

    private String name;

    private String description;

    private BigDecimal unitPrice;

    private String unit;

    private PartType partType;

    private Part(
            UUID id,
            String name,
            String description,
            BigDecimal unitPrice,
            String unit,
            PartType partType,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String createdBy,
            String updatedBy) {
        super(createdAt, updatedAt, createdBy, updatedBy);
        this.id = id;
        this.name = name;
        this.description = description;
        this.unitPrice = unitPrice;
        this.unit = unit;
        this.partType = partType;
    }

    /**
     * Registra uma peça nova no catálogo. {@code partType} ausente significa {@link PartType#PART}.
     */
    public static Part register(
            String name, String description, BigDecimal unitPrice, String unit, PartType partType) {
        validate(name, unitPrice, unit);
        return new Part(
                null, name, description, unitPrice, unit, partTypeOrDefault(partType),
                null, null, null, null);
    }

    /**
     * Reidrata uma peça já persistida, preservando identidade e auditoria; não revalida.
     */
    public static Part restore(
            UUID id,
            String name,
            String description,
            BigDecimal unitPrice,
            String unit,
            PartType partType,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String createdBy,
            String updatedBy) {
        return new Part(
                id, name, description, unitPrice, unit, partType, createdAt, updatedAt, createdBy, updatedBy);
    }

    /**
     * Substitui todos os dados da peça (PUT é substituição completa).
     */
    public void updateDetails(
            String name, String description, BigDecimal unitPrice, String unit, PartType partType) {
        validate(name, unitPrice, unit);
        this.name = name;
        this.description = description;
        this.unitPrice = unitPrice;
        this.unit = unit;
        this.partType = partTypeOrDefault(partType);
    }

    private static void validate(String name, BigDecimal unitPrice, String unit) {
        if (name == null || name.isBlank()) {
            throw new InvalidPartException("Part name is required");
        }
        if (unitPrice == null) {
            throw new InvalidPartException("Unit price is required");
        }
        if (unitPrice.signum() <= 0) {
            throw new InvalidPartException("Price must be greater than zero");
        }
        if (unit == null || unit.isBlank()) {
            throw new InvalidPartException("Unit is required");
        }
    }

    private static PartType partTypeOrDefault(PartType partType) {
        return partType != null ? partType : PartType.PART;
    }

    /**
     * Identidade só existe após a persistência: duas instâncias ainda sem id nunca são iguais.
     */
    @Override
    public final boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Part that)) {
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
        return Part.class.hashCode();
    }
}
