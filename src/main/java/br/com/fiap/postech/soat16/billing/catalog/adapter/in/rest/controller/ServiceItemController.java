package br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

import br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.dto.request.ServiceItemRequestDto;
import br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.dto.response.ServiceItemResponseDto;
import br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.mapper.ServiceCatalogRestMapper;
import br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.openapi.ServiceItemControllerDocs;
import br.com.fiap.postech.soat16.billing.catalog.application.ServiceItemService;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
@RolesAllowed({"ADMIN", "MECHANIC"})
public class ServiceItemController implements ServiceItemControllerDocs {

    public static final String SERVICES_PATH = "/admin/services";

    private final ServiceItemService service;

    @Override
    public Uni<List<ServiceItemResponseDto>> listAll() {
        return service.listAll()
                .map(results -> results.stream().map(ServiceCatalogRestMapper::toResponse).toList());
    }

    @Override
    public Uni<ServiceItemResponseDto> findById(UUID id) {
        return service.findById(ServiceCatalogRestMapper.toQuery(id))
                .map(ServiceCatalogRestMapper::toResponse);
    }

    @RolesAllowed("ADMIN")
    @Override
    public Uni<Response> create(ServiceItemRequestDto dto) {
        return service.create(ServiceCatalogRestMapper.toCreateCommand(dto))
                .map(ServiceCatalogRestMapper::toResponse)
                .map(created -> Response.created(URI.create(SERVICES_PATH + "/" + created.id()))
                        .entity(created)
                        .build());
    }

    @RolesAllowed("ADMIN")
    @Override
    public Uni<ServiceItemResponseDto> update(UUID id, ServiceItemRequestDto dto) {
        return service.update(ServiceCatalogRestMapper.toUpdateCommand(id, dto))
                .map(ServiceCatalogRestMapper::toResponse);
    }

    @RolesAllowed("ADMIN")
    @Override
    public Uni<Response> delete(UUID id) {
        return service.delete(ServiceCatalogRestMapper.toDeleteCommand(id))
                .replaceWith(Response.noContent().build());
    }
}
