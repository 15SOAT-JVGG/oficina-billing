package br.com.fiap.postech.soat16.billing.shared.adapter.out.persistence;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditJpaEntityListener.class)
public abstract class AuditableJpaEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", nullable = false, updatable = false, length = 100)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;
}
