package br.com.fiap.postech.soat16.billing.shared.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AuditJpaEntityListener — auditoria em UTC")
class AuditJpaEntityListenerTest {

    private static final class SampleJpaEntity extends AuditableJpaEntity {
    }

    private final AuditJpaEntityListener listener = new AuditJpaEntityListener();

    @Test
    @DisplayName("deve preencher criação e atualização iguais, em UTC, como system")
    void prePersist() {
        SampleJpaEntity entity = new SampleJpaEntity();

        listener.prePersist(entity);

        assertNotNull(entity.getCreatedAt());
        assertEquals(entity.getCreatedAt(), entity.getUpdatedAt());
        assertEquals(ZoneOffset.UTC, entity.getCreatedAt().getOffset());
        assertEquals("system", entity.getCreatedBy());
        assertEquals("system", entity.getUpdatedBy());
    }

    @Test
    @DisplayName("deve mover só a atualização, preservando a criação")
    void preUpdate() {
        SampleJpaEntity entity = new SampleJpaEntity();
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-01-01T10:00:00Z");
        entity.setCreatedAt(createdAt);
        entity.setCreatedBy("someone");

        listener.preUpdate(entity);

        assertEquals(createdAt, entity.getCreatedAt());
        assertEquals("someone", entity.getCreatedBy());
        assertTrue(entity.getUpdatedAt().isAfter(createdAt));
        assertEquals("system", entity.getUpdatedBy());
    }
}
