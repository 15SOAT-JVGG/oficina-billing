package br.com.fiap.postech.soat16.billing.estimate.application.port.out;

import java.util.Optional;
import java.util.UUID;

import br.com.fiap.postech.soat16.billing.estimate.application.result.CatalogItemSnapshotResult;

import io.smallrye.mutiny.Uni;

public interface CatalogLookupPort {
    Uni<Optional<CatalogItemSnapshotResult>> findService(UUID id);
    Uni<Optional<CatalogItemSnapshotResult>> findPart(UUID id);
}
