package br.com.fiap.postech.soat16.billing.catalog.adapter.out.persistence.mapper;

import br.com.fiap.postech.soat16.billing.catalog.adapter.out.persistence.entity.PartJpaEntity;
import br.com.fiap.postech.soat16.billing.catalog.domain.model.Part;
import br.com.fiap.postech.soat16.billing.shared.adapter.out.persistence.AuditPersistenceMapper;

public final class PartPersistenceMapper {

    private PartPersistenceMapper() {
    }

    public static Part toDomain(PartJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Part.restore(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getUnitPrice(),
                entity.getUnit(),
                entity.getPartType(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedBy());
    }

    public static PartJpaEntity toJpaEntity(Part part) {
        if (part == null) {
            return null;
        }
        var entity = new PartJpaEntity();
        entity.setId(part.getId());
        copyState(part, entity);
        AuditPersistenceMapper.copyToJpaEntity(part, entity);
        return entity;
    }

    public static void copyState(Part source, PartJpaEntity target) {
        target.setName(source.getName());
        target.setDescription(source.getDescription());
        target.setUnitPrice(source.getUnitPrice());
        target.setUnit(source.getUnit());
        target.setPartType(source.getPartType());
    }
}
