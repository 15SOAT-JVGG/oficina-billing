package br.com.fiap.postech.soat16.billing.catalog.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.billing.catalog.adapter.out.persistence.entity.ServiceItemJpaEntity;
import br.com.fiap.postech.soat16.billing.catalog.adapter.out.persistence.mapper.ServiceItemPersistenceMapper;
import br.com.fiap.postech.soat16.billing.catalog.application.port.out.ServiceCatalogPersistencePort;
import br.com.fiap.postech.soat16.billing.catalog.domain.model.ServiceItem;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class ServiceItemRepository
        implements PanacheRepositoryBase<ServiceItemJpaEntity, UUID>, ServiceCatalogPersistencePort {

    @Override
    public Uni<List<ServiceItem>> listAllServiceItems() {
        return listAll().map(entities -> entities.stream()
                .map(ServiceItemPersistenceMapper::toDomain)
                .toList());
    }

    @Override
    public Uni<ServiceItem> findServiceItemById(UUID id) {
        return findById(id).map(ServiceItemPersistenceMapper::toDomain);
    }

    @Override
    public Uni<ServiceItem> save(ServiceItem serviceItem) {
        if (serviceItem.getId() == null) {
            return persist(ServiceItemPersistenceMapper.toJpaEntity(serviceItem))
                    .map(ServiceItemPersistenceMapper::toDomain);
        }
        return findById(serviceItem.getId())
                .onItem().ifNotNull().transform(entity -> {
                    ServiceItemPersistenceMapper.copyState(serviceItem, entity);
                    return ServiceItemPersistenceMapper.toDomain(entity);
                });
    }

    @Override
    public Uni<Boolean> deleteServiceItemById(UUID id) {
        return deleteById(id);
    }
}
