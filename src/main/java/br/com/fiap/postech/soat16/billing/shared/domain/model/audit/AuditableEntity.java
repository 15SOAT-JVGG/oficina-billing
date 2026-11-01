package br.com.fiap.postech.soat16.billing.shared.domain.model.audit;

import java.time.OffsetDateTime;

import lombok.Getter;

/**
 * Estado de auditoria comum a toda entidade persistida. É preenchido pela infraestrutura ao
 * gravar e só volta ao domínio pela reidratação ({@code restore(...)} de cada agregado); por isso
 * não há setter: o domínio nunca decide quando foi criado.
 */
@Getter
public abstract class AuditableEntity {

    private final OffsetDateTime createdAt;

    private final OffsetDateTime updatedAt;

    private final String createdBy;

    private final String updatedBy;

    protected AuditableEntity() {
        this(null, null, null, null);
    }

    protected AuditableEntity(
            OffsetDateTime createdAt, OffsetDateTime updatedAt, String createdBy, String updatedBy) {
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }
}
