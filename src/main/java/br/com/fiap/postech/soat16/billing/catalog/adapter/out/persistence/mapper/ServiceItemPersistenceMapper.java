package br.com.fiap.postech.soat16.billing.catalog.adapter.out.persistence.mapper;

import br.com.fiap.postech.soat16.billing.catalog.adapter.out.persistence.entity.ServiceItemJpaEntity;
import br.com.fiap.postech.soat16.billing.catalog.domain.model.ServiceItem;
import br.com.fiap.postech.soat16.billing.shared.adapter.out.persistence.AuditPersistenceMapper;

public final class ServiceItemPersistenceMapper {

    private ServiceItemPersistenceMapper() {
    }

    public static ServiceItem toDomain(ServiceItemJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return ServiceItem.restore(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getBasePrice(),
                entity.getEstimatedDurationMinutes(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedBy());
    }

    public static ServiceItemJpaEntity toJpaEntity(ServiceItem serviceItem) {
        if (serviceItem == null) {
            return null;
        }
        var entity = new ServiceItemJpaEntity();
        entity.setId(serviceItem.getId());
        copyState(serviceItem, entity);
        AuditPersistenceMapper.copyToJpaEntity(serviceItem, entity);
        return entity;
    }

    public static void copyState(ServiceItem source, ServiceItemJpaEntity target) {
        target.setName(source.getName());
        target.setDescription(source.getDescription());
        target.setBasePrice(source.getBasePrice());
        target.setEstimatedDurationMinutes(source.getEstimatedDurationMinutes());
        target.setActive(source.isActive());
    }
}
