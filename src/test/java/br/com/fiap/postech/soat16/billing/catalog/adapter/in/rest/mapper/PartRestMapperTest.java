package br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.dto.request.PartRequestDto;
import br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.dto.response.PartResponseDto;
import br.com.fiap.postech.soat16.billing.catalog.application.command.CreatePartCommand;
import br.com.fiap.postech.soat16.billing.catalog.application.command.UpdatePartCommand;
import br.com.fiap.postech.soat16.billing.catalog.application.result.PartResult;
import br.com.fiap.postech.soat16.billing.catalog.domain.model.PartType;

@DisplayName("PartRestMapper — DTO ↔ Command/Result")
class PartRestMapperTest {

    private static final UUID ID = UUID.randomUUID();
    private static final PartRequestDto REQUEST =
            new PartRequestDto("Oleo 5W30", "Sintetico", new BigDecimal("59.90"), "L", PartType.SUPPLY);

    @Test
    @DisplayName("deve mapear o request para o comando de criação campo a campo")
    void toCreateCommand() {
        CreatePartCommand command = PartRestMapper.toCreateCommand(REQUEST);

        assertEquals(new CreatePartCommand("Oleo 5W30", "Sintetico", new BigDecimal("59.90"), "L", PartType.SUPPLY), command);
    }

    @Test
    @DisplayName("deve mapear o request e o id para o comando de atualização")
    void toUpdateCommand() {
        UpdatePartCommand command = PartRestMapper.toUpdateCommand(ID, REQUEST);

        assertEquals(
                new UpdatePartCommand(ID, "Oleo 5W30", "Sintetico", new BigDecimal("59.90"), "L", PartType.SUPPLY), command);
    }

    @Test
    @DisplayName("deve preservar partType nulo no comando (o agregado aplica o padrão)")
    void keepsNullPartType() {
        PartRequestDto request = new PartRequestDto("x", null, BigDecimal.ONE, "UN", null);

        assertNull(PartRestMapper.toCreateCommand(request).partType());
        assertNull(PartRestMapper.toUpdateCommand(ID, request).description());
    }

    @Test
    @DisplayName("deve mapear id para query e comando de remoção")
    void toQueryAndDelete() {
        assertEquals(ID, PartRestMapper.toQuery(ID).id());
        assertEquals(ID, PartRestMapper.toDeleteCommand(ID).id());
    }

    @Test
    @DisplayName("deve mapear o resultado para a resposta campo a campo")
    void toResponse() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-01-01T10:00:00Z");
        PartResult result = new PartResult(ID, "Filtro", null, new BigDecimal("35.90"), "UN", PartType.PART, createdAt);

        PartResponseDto response = PartRestMapper.toResponse(result);

        assertEquals(new PartResponseDto(ID, "Filtro", null, new BigDecimal("35.90"), "UN", PartType.PART, createdAt), response);
    }
}
