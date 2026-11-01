package br.com.fiap.postech.soat16.billing.catalog.application;

import static java.lang.Boolean.FALSE;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.billing.catalog.application.command.CreateServiceItemCommand;
import br.com.fiap.postech.soat16.billing.catalog.application.command.DeleteServiceItemCommand;
import br.com.fiap.postech.soat16.billing.catalog.application.command.FindServiceItemQuery;
import br.com.fiap.postech.soat16.billing.catalog.application.command.UpdateServiceItemCommand;
import br.com.fiap.postech.soat16.billing.catalog.application.port.out.ServiceCatalogPersistencePort;
import br.com.fiap.postech.soat16.billing.catalog.application.result.ServiceItemResult;
import br.com.fiap.postech.soat16.billing.catalog.domain.exception.ServiceItemNotFoundException;
import br.com.fiap.postech.soat16.billing.catalog.domain.model.ServiceItem;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class ServiceItemService {

    private final ServiceCatalogPersistencePort serviceCatalogPersistence;

    @WithSession
    public Uni<List<ServiceItemResult>> listAll() {
        return serviceCatalogPersistence.listAllServiceItems()
                .map(items -> items.stream().map(ServiceItemResult::from).toList());
    }

    @WithSession
    public Uni<ServiceItemResult> findById(FindServiceItemQuery query) {
        return loadOrFail(query.id()).map(ServiceItemResult::from);
    }

    @WithTransaction
    public Uni<ServiceItemResult> create(CreateServiceItemCommand command) {
        ServiceItem serviceItem = ServiceItem.register(
                command.name(),
                command.description(),
                command.basePrice(),
                command.estimatedDurationMinutes(),
                command.active());
        return serviceCatalogPersistence.save(serviceItem).map(ServiceItemResult::from);
    }

    @WithTransaction
    public Uni<ServiceItemResult> update(UpdateServiceItemCommand command) {
        return loadOrFail(command.id())
                .flatMap(serviceItem -> {
                    serviceItem.updateDetails(
                            command.name(),
                            command.description(),
                            command.basePrice(),
                            command.estimatedDurationMinutes(),
                            command.active());
                    return serviceCatalogPersistence.save(serviceItem);
                })
                .map(ServiceItemResult::from);
    }

    @WithTransaction
    public Uni<Void> delete(DeleteServiceItemCommand command) {
        return serviceCatalogPersistence.deleteServiceItemById(command.id())
                .flatMap(deleted -> FALSE.equals(deleted)
                        ? Uni.createFrom().failure(new ServiceItemNotFoundException(command.id()))
                        : Uni.createFrom().voidItem());
    }

    private Uni<ServiceItem> loadOrFail(UUID id) {
        return serviceCatalogPersistence.findServiceItemById(id)
                .onItem().ifNull().failWith(() -> new ServiceItemNotFoundException(id));
    }
}
