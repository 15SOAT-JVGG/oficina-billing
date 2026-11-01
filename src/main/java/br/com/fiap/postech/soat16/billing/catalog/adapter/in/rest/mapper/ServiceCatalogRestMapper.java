package br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.mapper;

import java.util.UUID;

import br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.dto.request.ServiceItemRequestDto;
import br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.dto.response.ServiceItemResponseDto;
import br.com.fiap.postech.soat16.billing.catalog.application.command.CreateServiceItemCommand;
import br.com.fiap.postech.soat16.billing.catalog.application.command.DeleteServiceItemCommand;
import br.com.fiap.postech.soat16.billing.catalog.application.command.FindServiceItemQuery;
import br.com.fiap.postech.soat16.billing.catalog.application.command.UpdateServiceItemCommand;
import br.com.fiap.postech.soat16.billing.catalog.application.result.ServiceItemResult;

public final class ServiceCatalogRestMapper {

    private ServiceCatalogRestMapper() {
    }

    public static CreateServiceItemCommand toCreateCommand(ServiceItemRequestDto request) {
        return new CreateServiceItemCommand(
                request.name(),
                request.description(),
                request.basePrice(),
                request.estimatedDurationMinutes(),
                request.active());
    }

    public static UpdateServiceItemCommand toUpdateCommand(UUID id, ServiceItemRequestDto request) {
        return new UpdateServiceItemCommand(
                id,
                request.name(),
                request.description(),
                request.basePrice(),
                request.estimatedDurationMinutes(),
                request.active());
    }

    public static FindServiceItemQuery toQuery(UUID id) {
        return new FindServiceItemQuery(id);
    }

    public static DeleteServiceItemCommand toDeleteCommand(UUID id) {
        return new DeleteServiceItemCommand(id);
    }

    public static ServiceItemResponseDto toResponse(ServiceItemResult result) {
        return new ServiceItemResponseDto(
                result.id(),
                result.name(),
                result.description(),
                result.basePrice(),
                result.estimatedDurationMinutes(),
                result.active(),
                result.createdAt());
    }
}
