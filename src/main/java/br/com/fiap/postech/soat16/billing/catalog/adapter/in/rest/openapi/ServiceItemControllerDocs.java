package br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.openapi;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.dto.request.ServiceItemRequestDto;
import br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.dto.response.ServiceItemResponseDto;
import br.com.fiap.postech.soat16.billing.shared.adapter.in.rest.dto.ApiErrorResponseDto;

import io.smallrye.mutiny.Uni;

@Path("/admin/services")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Service Catalog", description = "Catalog of services offered by the workshop")
public interface ServiceItemControllerDocs {

    String UNAUTHORIZED = "Missing or invalid token";
    String FORBIDDEN = "Insufficient role";
    String NOT_FOUND = "Service not found";

    @GET
    @Operation(summary = "List services", description = "Returns all registered catalog services.")
    @APIResponse(
            responseCode = "200",
            description = "Services returned successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ServiceItemResponseDto.class, type = SchemaType.ARRAY)))
    @APIResponse(responseCode = "401", description = UNAUTHORIZED)
    @APIResponse(responseCode = "403", description = FORBIDDEN)
    Uni<List<ServiceItemResponseDto>> listAll();

    @GET
    @Path("/{id}")
    @Operation(summary = "Find service by ID", description = "Returns a single catalog service by its identifier.")
    @APIResponse(
            responseCode = "200",
            description = "Service found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ServiceItemResponseDto.class)))
    @APIResponse(responseCode = "401", description = UNAUTHORIZED)
    @APIResponse(responseCode = "403", description = FORBIDDEN)
    @APIResponse(
            responseCode = "404",
            description = NOT_FOUND,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ApiErrorResponseDto.class)))
    Uni<ServiceItemResponseDto> findById(
            @Parameter(name = "id", description = "Service identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") UUID id);

    @POST
    @Operation(summary = "Register new service", description = "Creates a new catalog service. Restricted to ADMIN.")
    @APIResponse(
            responseCode = "201",
            description = "Service created successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ServiceItemResponseDto.class)))
    @APIResponse(responseCode = "400", description = "Invalid request body")
    @APIResponse(responseCode = "401", description = UNAUTHORIZED)
    @APIResponse(responseCode = "403", description = FORBIDDEN)
    Uni<Response> create(
            @RequestBody(description = "Service data for registration") @Valid ServiceItemRequestDto body);

    @PUT
    @Path("/{id}")
    @Operation(
            summary = "Update service",
            description = "Fully replaces the data of a catalog service. Restricted to ADMIN.")
    @APIResponse(
            responseCode = "200",
            description = "Service updated successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ServiceItemResponseDto.class)))
    @APIResponse(responseCode = "400", description = "Invalid request body")
    @APIResponse(responseCode = "401", description = UNAUTHORIZED)
    @APIResponse(responseCode = "403", description = FORBIDDEN)
    @APIResponse(
            responseCode = "404",
            description = NOT_FOUND,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ApiErrorResponseDto.class)))
    Uni<ServiceItemResponseDto> update(
            @Parameter(name = "id", description = "Service identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") UUID id,
            @RequestBody(description = "Updated service data") @Valid ServiceItemRequestDto body);

    @DELETE
    @Path("/{id}")
    @Operation(
            summary = "Delete service",
            description = "Permanently removes a catalog service by its identifier. Restricted to ADMIN.")
    @APIResponse(responseCode = "204", description = "Service deleted successfully")
    @APIResponse(responseCode = "401", description = UNAUTHORIZED)
    @APIResponse(responseCode = "403", description = FORBIDDEN)
    @APIResponse(
            responseCode = "404",
            description = NOT_FOUND,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ApiErrorResponseDto.class)))
    Uni<Response> delete(
            @Parameter(name = "id", description = "Service identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") UUID id);
}
