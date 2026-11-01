package br.com.fiap.postech.soat16.billing.catalog.adapter.out.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.billing.catalog.adapter.out.persistence.entity.PartJpaEntity;
import br.com.fiap.postech.soat16.billing.catalog.domain.model.Part;
import br.com.fiap.postech.soat16.billing.catalog.domain.model.PartType;

@DisplayName("PartPersistenceMapper — ida e volta entre agregado e entidade JPA")
class PartPersistenceMapperTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-01-01T10:00:00Z");
    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse("2026-01-02T10:00:00Z");

    @Test
    @DisplayName("deve reidratar o agregado com todos os campos e a auditoria")
    void shouldMapToDomain() {
        UUID id = UUID.randomUUID();
        var entity = new PartJpaEntity();
        entity.setId(id);
        entity.setName("Oleo 5W30");
        entity.setDescription("Sintetico");
        entity.setUnitPrice(new BigDecimal("59.90"));
        entity.setUnit("L");
        entity.setPartType(PartType.SUPPLY);
        entity.setCreatedAt(CREATED_AT);
        entity.setUpdatedAt(UPDATED_AT);
        entity.setCreatedBy("system");
        entity.setUpdatedBy("admin");

        Part part = PartPersistenceMapper.toDomain(entity);

        assertEquals(id, part.getId());
        assertEquals("Oleo 5W30", part.getName());
        assertEquals("Sintetico", part.getDescription());
        assertEquals(new BigDecimal("59.90"), part.getUnitPrice());
        assertEquals("L", part.getUnit());
        assertEquals(PartType.SUPPLY, part.getPartType());
        assertEquals(CREATED_AT, part.getCreatedAt());
        assertEquals(UPDATED_AT, part.getUpdatedAt());
        assertEquals("system", part.getCreatedBy());
        assertEquals("admin", part.getUpdatedBy());
    }

    @Test
    @DisplayName("deve mapear o agregado novo para uma entidade sem id nem auditoria")
    void shouldMapNewAggregateToEntity() {
        Part part = Part.register("Filtro de oleo", null, new BigDecimal("35.90"), "UN", null);

        PartJpaEntity entity = PartPersistenceMapper.toJpaEntity(part);

        assertNull(entity.getId());
        assertEquals("Filtro de oleo", entity.getName());
        assertNull(entity.getDescription());
        assertEquals(new BigDecimal("35.90"), entity.getUnitPrice());
        assertEquals("UN", entity.getUnit());
        assertEquals(PartType.PART, entity.getPartType());
        assertNull(entity.getCreatedAt());
    }

    @Test
    @DisplayName("deve copiar identidade e auditoria do agregado reidratado")
    void shouldMapRestoredAggregateToEntity() {
        UUID id = UUID.randomUUID();
        Part part = Part.restore(
                id, "Filtro", null, new BigDecimal("35.90"), "UN", PartType.PART, CREATED_AT, UPDATED_AT, "a", "b");

        PartJpaEntity entity = PartPersistenceMapper.toJpaEntity(part);

        assertEquals(id, entity.getId());
        assertEquals(CREATED_AT, entity.getCreatedAt());
        assertEquals(UPDATED_AT, entity.getUpdatedAt());
        assertEquals("a", entity.getCreatedBy());
        assertEquals("b", entity.getUpdatedBy());
    }

    @Test
    @DisplayName("copyState deve atualizar só o estado de negócio, preservando id e auditoria")
    void copyStateShouldKeepIdentityAndAudit() {
        UUID id = UUID.randomUUID();
        var entity = new PartJpaEntity();
        entity.setId(id);
        entity.setCreatedAt(CREATED_AT);
        Part part = Part.register("Novo", "nova desc", new BigDecimal("1.00"), "CX", PartType.SUPPLY);

        PartPersistenceMapper.copyState(part, entity);

        assertEquals(id, entity.getId());
        assertEquals(CREATED_AT, entity.getCreatedAt());
        assertEquals("Novo", entity.getName());
        assertEquals("nova desc", entity.getDescription());
        assertEquals(new BigDecimal("1.00"), entity.getUnitPrice());
        assertEquals("CX", entity.getUnit());
        assertEquals(PartType.SUPPLY, entity.getPartType());
    }

    @Test
    @DisplayName("deve tratar null nos dois sentidos")
    void shouldHandleNull() {
        assertNull(PartPersistenceMapper.toDomain(null));
        assertNull(PartPersistenceMapper.toJpaEntity(null));
    }
}
