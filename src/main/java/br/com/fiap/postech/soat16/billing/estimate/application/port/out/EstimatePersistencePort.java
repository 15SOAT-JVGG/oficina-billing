package br.com.fiap.postech.soat16.billing.estimate.application.port.out;

import br.com.fiap.postech.soat16.billing.estimate.domain.model.Estimate;

import io.smallrye.mutiny.Uni;

@FunctionalInterface
public interface EstimatePersistencePort {
    Uni<Void> save(Estimate estimate);
}
