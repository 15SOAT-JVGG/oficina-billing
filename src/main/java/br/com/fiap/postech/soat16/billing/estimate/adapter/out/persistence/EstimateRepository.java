package br.com.fiap.postech.soat16.billing.estimate.adapter.out.persistence;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.billing.estimate.adapter.out.persistence.entity.EstimateJpaEntity;
import br.com.fiap.postech.soat16.billing.estimate.adapter.out.persistence.mapper.EstimatePersistenceMapper;
import br.com.fiap.postech.soat16.billing.estimate.application.port.out.EstimatePersistencePort;
import br.com.fiap.postech.soat16.billing.estimate.domain.model.Estimate;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class EstimateRepository implements PanacheRepositoryBase<EstimateJpaEntity, UUID>, EstimatePersistencePort {

    @Override
    public Uni<Void> save(Estimate estimate) {
        return persist(EstimatePersistenceMapper.toJpaEntity(estimate)).replaceWithVoid();
    }
}
