package br.com.fiap.postech.soat16.billing.estimate.adapter.out.persistence.mapper;

import java.util.UUID;

import br.com.fiap.postech.soat16.billing.estimate.adapter.out.persistence.entity.EstimateItemJpaEntity;
import br.com.fiap.postech.soat16.billing.estimate.adapter.out.persistence.entity.EstimateJpaEntity;
import br.com.fiap.postech.soat16.billing.estimate.domain.model.Estimate;

public final class EstimatePersistenceMapper {

    private EstimatePersistenceMapper() { }

    public static EstimateJpaEntity toJpaEntity(Estimate estimate) {
        var entity = new EstimateJpaEntity();
        entity.setId(estimate.getId());
        entity.setWorkOrderId(estimate.getWorkOrderId());
        entity.setOpeningEventId(estimate.getOpeningEventId());
        entity.setSagaId(estimate.getSagaId());
        entity.setCorrelationId(estimate.getCorrelationId());
        entity.setTotalAmount(estimate.getTotalAmount());
        entity.setCreatedAt(estimate.getCreatedAt());
        entity.setItems(estimate.getItems().stream().map(item -> {
            var itemEntity = new EstimateItemJpaEntity();
            itemEntity.setId(UUID.randomUUID());
            itemEntity.setEstimate(entity);
            itemEntity.setItemType(item.itemType());
            itemEntity.setCatalogItemId(item.catalogItemId());
            itemEntity.setName(item.name());
            itemEntity.setQuantity(item.quantity());
            itemEntity.setUnitPrice(item.unitPrice());
            return itemEntity;
        }).toList());
        return entity;
    }
}
