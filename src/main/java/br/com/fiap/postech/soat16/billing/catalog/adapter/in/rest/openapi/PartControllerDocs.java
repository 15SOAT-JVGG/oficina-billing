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

import br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.dto.request.PartRequestDto;
import br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.dto.response.PartResponseDto;
import br.com.fiap.postech.soat16.billing.shared.adapter.in.rest.dto.ApiErrorResponseDto;

import io.smallrye.mutiny.Uni;

/**
 * Sem as rotas de estoque do monólito ({@code GET /low-stock}, {@code PATCH /{id}/stock}): o saldo
 * é do Execution (spec #106).
 */
@Path(PartControllerDocs.PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Parts and Supplies", description = "Parts and supplies catalog with unit prices")
public interface PartControllerDocs {

    String PATH = "/admin/parts";

    String UNAUTHORIZED = "Missing or invalid token";
    String FORBIDDEN = "Insufficient role";
    String NOT_FOUND = "Part/supply not found";

    @GET
    @Operation(summary = "List all parts/supplies", description = "Returns all registered parts and supplies.")
    @APIResponse(
            responseCode = "200",
            description = "Parts/supplies returned successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = PartResponseDto.class, type = SchemaType.ARRAY)))
    @APIResponse(responseCode = "401", description = UNAUTHORIZED)
    @APIResponse(responseCode = "403", description = FORBIDDEN)
    Uni<List<PartResponseDto>> listAll();

    @GET
    @Path("/{id}")
    @Operation(summary = "Find part/supply by ID", description = "Returns a single part/supply by its identifier.")
    @APIResponse(
            responseCode = "200",
            description = "Part/supply found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = PartResponseDto.class)))
    @APIResponse(responseCode = "401", description = UNAUTHORIZED)
    @APIResponse(responseCode = "403", description = FORBIDDEN)
    @APIResponse(
            responseCode = "404",
            description = NOT_FOUND,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ApiErrorResponseDto.class)))
    Uni<PartResponseDto> findById(
            @Parameter(name = "id", description = "Part/supply identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") UUID id);

    @POST
    @Operation(
            summary = "Register new part/supply",
            description = "Creates a new part/supply record. Restricted to ADMIN.")
    @APIResponse(
            responseCode = "201",
            description = "Part/supply created successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = PartResponseDto.class)))
    @APIResponse(responseCode = "400", description = "Invalid request body")
    @APIResponse(responseCode = "401", description = UNAUTHORIZED)
    @APIResponse(responseCode = "403", description = FORBIDDEN)
    Uni<Response> create(
            @RequestBody(description = "Part/supply data for registration") @Valid PartRequestDto body);

    @PUT
    @Path("/{id}")
    @Operation(
            summary = "Update part/supply",
            description = "Fully replaces the data of a part/supply. Restricted to ADMIN.")
    @APIResponse(
            responseCode = "200",
            description = "Part/supply updated successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = PartResponseDto.class)))
    @APIResponse(responseCode = "400", description = "Invalid request body")
    @APIResponse(responseCode = "401", description = UNAUTHORIZED)
    @APIResponse(responseCode = "403", description = FORBIDDEN)
    @APIResponse(
            responseCode = "404",
            description = NOT_FOUND,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ApiErrorResponseDto.class)))
    Uni<PartResponseDto> update(
            @Parameter(name = "id", description = "Part/supply identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") UUID id,
            @RequestBody(description = "Updated part/supply data") @Valid PartRequestDto body);

    @DELETE
    @Path("/{id}")
    @Operation(
            summary = "Delete part/supply",
            description = "Permanently removes a part/supply by its identifier. Restricted to ADMIN.")
    @APIResponse(responseCode = "204", description = "Part/supply deleted successfully")
    @APIResponse(responseCode = "401", description = UNAUTHORIZED)
    @APIResponse(responseCode = "403", description = FORBIDDEN)
    @APIResponse(
            responseCode = "404",
            description = NOT_FOUND,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ApiErrorResponseDto.class)))
    Uni<Response> delete(
            @Parameter(name = "id", description = "Part/supply identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") UUID id);
}
