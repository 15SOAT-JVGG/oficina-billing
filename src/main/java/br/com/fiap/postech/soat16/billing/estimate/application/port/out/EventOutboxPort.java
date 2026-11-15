package br.com.fiap.postech.soat16.billing.estimate.application.port.out;

import br.com.fiap.postech.soat16.billing.estimate.application.command.EstimateCreationFailureCommand;
import br.com.fiap.postech.soat16.billing.estimate.domain.model.Estimate;

import io.smallrye.mutiny.Uni;

public interface EventOutboxPort {
    Uni<Void> recordCreated(Estimate estimate, String traceparent);
    Uni<Void> recordFailed(EstimateCreationFailureCommand failure, String traceparent);
}
