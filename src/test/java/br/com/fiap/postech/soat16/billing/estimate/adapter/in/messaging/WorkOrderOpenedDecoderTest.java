package br.com.fiap.postech.soat16.billing.estimate.adapter.in.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("WorkOrderOpenedDecoder — contrato recebido")
class WorkOrderOpenedDecoderTest {

    private WorkOrderOpenedDecoder decoder;

    @BeforeEach
    void setUp() {
        decoder = new WorkOrderOpenedDecoder();
        decoder.mapper = new ObjectMapper();
        decoder.initialize();
    }

    @Test
    @DisplayName("aceita o exemplo versionado e preserva o atributo traceparent")
    void decodesVersionedExample() throws IOException {
        String payload = Files.readString(Path.of("docs/events/v1/work-order-opened.example.json"));

        var command = decoder.decode(payload, "00-11111111111111111111111111111111-2222222222222222-01");

        assertEquals(1, command.services().size());
        assertEquals(1, command.parts().size());
        assertEquals(2, command.parts().getFirst().quantity());
        assertEquals("00-11111111111111111111111111111111-2222222222222222-01", command.traceparent());
    }

    @Test
    @DisplayName("rejeita campos fora do schema e JSON inválido")
    void rejectsInvalidPayloads() throws IOException {
        String payload = Files.readString(Path.of("docs/events/v1/work-order-opened.example.json"));
        String withUnknownField = payload.replaceFirst("\\{", "{\"unexpected\":true,");

        assertThrows(IllegalArgumentException.class, () -> decoder.decode(withUnknownField, null));
        assertThrows(IllegalArgumentException.class, () -> decoder.decode("not-json", null));
    }
}
