package br.com.fiap.postech.soat16.billing.catalog.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.billing.catalog.adapter.out.persistence.entity.PartJpaEntity;
import br.com.fiap.postech.soat16.billing.catalog.adapter.out.persistence.mapper.PartPersistenceMapper;
import br.com.fiap.postech.soat16.billing.catalog.application.port.out.PartPersistencePort;
import br.com.fiap.postech.soat16.billing.catalog.domain.model.Part;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class PartRepository implements PanacheRepositoryBase<PartJpaEntity, UUID>, PartPersistencePort {

    @Override
    public Uni<List<Part>> listAllParts() {
        return listAll().map(entities -> entities.stream()
                .map(PartPersistenceMapper::toDomain)
                .toList());
    }

    @Override
    public Uni<Part> findPartById(UUID id) {
        return findById(id).map(PartPersistenceMapper::toDomain);
    }

    @Override
    public Uni<Part> save(Part part) {
        if (part.getId() == null) {
            return persist(PartPersistenceMapper.toJpaEntity(part))
                    .map(PartPersistenceMapper::toDomain);
        }
        return findById(part.getId())
                .onItem().ifNotNull().transform(entity -> {
                    PartPersistenceMapper.copyState(part, entity);
                    return PartPersistenceMapper.toDomain(entity);
                });
    }

    @Override
    public Uni<Boolean> deletePartById(UUID id) {
        return deleteById(id);
    }
}
