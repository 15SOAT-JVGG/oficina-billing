package br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

import br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.dto.request.PartRequestDto;
import br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.dto.response.PartResponseDto;
import br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.mapper.PartRestMapper;
import br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.openapi.PartControllerDocs;
import br.com.fiap.postech.soat16.billing.catalog.application.PartService;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
@RolesAllowed({"ADMIN", "MECHANIC"})
public class PartController implements PartControllerDocs {

    public static final String PARTS_PATH = "/admin/parts";

    private final PartService service;

    @Override
    public Uni<List<PartResponseDto>> listAll() {
        return service.listAll()
                .map(results -> results.stream().map(PartRestMapper::toResponse).toList());
    }

    @Override
    public Uni<PartResponseDto> findById(UUID id) {
        return service.findById(PartRestMapper.toQuery(id))
                .map(PartRestMapper::toResponse);
    }

    @RolesAllowed("ADMIN")
    @Override
    public Uni<Response> create(PartRequestDto dto) {
        return service.create(PartRestMapper.toCreateCommand(dto))
                .map(PartRestMapper::toResponse)
                .map(created -> Response.created(URI.create(PARTS_PATH + "/" + created.id()))
                        .entity(created)
                        .build());
    }

    @RolesAllowed("ADMIN")
    @Override
    public Uni<PartResponseDto> update(UUID id, PartRequestDto dto) {
        return service.update(PartRestMapper.toUpdateCommand(id, dto))
                .map(PartRestMapper::toResponse);
    }

    @RolesAllowed("ADMIN")
    @Override
    public Uni<Response> delete(UUID id) {
        return service.delete(PartRestMapper.toDeleteCommand(id))
                .replaceWith(Response.noContent().build());
    }
}
