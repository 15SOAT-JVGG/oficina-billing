package br.com.fiap.postech.soat16.billing.catalog.adapter.out.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.billing.catalog.adapter.out.persistence.entity.ServiceItemJpaEntity;
import br.com.fiap.postech.soat16.billing.catalog.domain.model.ServiceItem;

@DisplayName("ServiceItemPersistenceMapper — ida e volta entre agregado e entidade JPA")
class ServiceItemPersistenceMapperTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-01-01T10:00:00Z");
    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse("2026-01-02T10:00:00Z");

    @Test
    @DisplayName("deve reidratar o agregado com todos os campos e a auditoria")
    void shouldMapToDomain() {
        UUID id = UUID.randomUUID();
        var entity = new ServiceItemJpaEntity();
        entity.setId(id);
        entity.setName("Troca de oleo");
        entity.setDescription("desc");
        entity.setBasePrice(new BigDecimal("80.00"));
        entity.setEstimatedDurationMinutes(30);
        entity.setActive(false);
        entity.setCreatedAt(CREATED_AT);
        entity.setUpdatedAt(UPDATED_AT);
        entity.setCreatedBy("system");
        entity.setUpdatedBy("admin");

        ServiceItem serviceItem = ServiceItemPersistenceMapper.toDomain(entity);

        assertEquals(id, serviceItem.getId());
        assertEquals("Troca de oleo", serviceItem.getName());
        assertEquals("desc", serviceItem.getDescription());
        assertEquals(new BigDecimal("80.00"), serviceItem.getBasePrice());
        assertEquals(30, serviceItem.getEstimatedDurationMinutes());
        assertEquals(false, serviceItem.isActive());
        assertEquals(CREATED_AT, serviceItem.getCreatedAt());
        assertEquals(UPDATED_AT, serviceItem.getUpdatedAt());
        assertEquals("system", serviceItem.getCreatedBy());
        assertEquals("admin", serviceItem.getUpdatedBy());
    }

    @Test
    @DisplayName("deve mapear o agregado novo para uma entidade sem id nem auditoria")
    void shouldMapNewAggregateToEntity() {
        ServiceItem serviceItem = ServiceItem.register("Troca de oleo", null, new BigDecimal("80.00"), 30, true);

        ServiceItemJpaEntity entity = ServiceItemPersistenceMapper.toJpaEntity(serviceItem);

        assertNull(entity.getId());
        assertEquals("Troca de oleo", entity.getName());
        assertNull(entity.getDescription());
        assertEquals(new BigDecimal("80.00"), entity.getBasePrice());
        assertEquals(30, entity.getEstimatedDurationMinutes());
        assertEquals(true, entity.isActive());
        assertNull(entity.getCreatedAt());
        assertNull(entity.getCreatedBy());
    }

    @Test
    @DisplayName("deve copiar identidade e auditoria do agregado reidratado")
    void shouldMapRestoredAggregateToEntity() {
        UUID id = UUID.randomUUID();
        ServiceItem serviceItem = ServiceItem.restore(
                id, "Alinhamento", "desc", new BigDecimal("120.00"), 45, true, CREATED_AT, UPDATED_AT, "a", "b");

        ServiceItemJpaEntity entity = ServiceItemPersistenceMapper.toJpaEntity(serviceItem);

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
        var entity = new ServiceItemJpaEntity();
        entity.setId(id);
        entity.setCreatedAt(CREATED_AT);
        entity.setCreatedBy("system");
        ServiceItem serviceItem = ServiceItem.register("Novo nome", "nova desc", new BigDecimal("99.90"), 10, false);

        ServiceItemPersistenceMapper.copyState(serviceItem, entity);

        assertEquals(id, entity.getId());
        assertEquals(CREATED_AT, entity.getCreatedAt());
        assertEquals("system", entity.getCreatedBy());
        assertEquals("Novo nome", entity.getName());
        assertEquals("nova desc", entity.getDescription());
        assertEquals(new BigDecimal("99.90"), entity.getBasePrice());
        assertEquals(10, entity.getEstimatedDurationMinutes());
        assertEquals(false, entity.isActive());
    }

    @Test
    @DisplayName("deve tratar null nos dois sentidos")
    void shouldHandleNull() {
        assertNull(ServiceItemPersistenceMapper.toDomain(null));
        assertNull(ServiceItemPersistenceMapper.toJpaEntity(null));
    }
}
