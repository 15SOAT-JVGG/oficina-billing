package br.com.fiap.postech.soat16.billing.catalog.application.port.out;

import java.util.List;
import java.util.UUID;

import br.com.fiap.postech.soat16.billing.catalog.domain.model.ServiceItem;

import io.smallrye.mutiny.Uni;

public interface ServiceCatalogPersistencePort {

    Uni<List<ServiceItem>> listAllServiceItems();

    /**
     * Emite {@code null} quando não há serviço com o id informado.
     */
    Uni<ServiceItem> findServiceItemById(UUID id);

    /**
     * Insere quando o agregado ainda não tem id; atualiza quando tem. Emite {@code null} se o id
     * informado não existir mais.
     */
    Uni<ServiceItem> save(ServiceItem serviceItem);

    /**
     * Emite {@code false} quando não havia serviço com o id informado.
     */
    Uni<Boolean> deleteServiceItemById(UUID id);
}
