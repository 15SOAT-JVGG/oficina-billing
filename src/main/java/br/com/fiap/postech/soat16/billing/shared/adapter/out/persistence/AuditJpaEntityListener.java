package br.com.fiap.postech.soat16.billing.shared.adapter.out.persistence;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * Preenche a auditoria em UTC. O autor fica como "system", como no monólito; gravar o upn do JWT
 * é a pendência 3 de docs/architecture/domain-decisions.md.
 */
public class AuditJpaEntityListener {

    private static final String SYSTEM_USER = "system";

    @PrePersist
    public void prePersist(AuditableJpaEntity entity) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(SYSTEM_USER);
        entity.setUpdatedBy(SYSTEM_USER);
    }

    @PreUpdate
    public void preUpdate(AuditableJpaEntity entity) {
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        entity.setUpdatedBy(SYSTEM_USER);
    }
}
