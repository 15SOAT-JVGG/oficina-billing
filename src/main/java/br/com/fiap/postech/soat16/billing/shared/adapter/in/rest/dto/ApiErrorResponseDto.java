package br.com.fiap.postech.soat16.billing.shared.adapter.in.rest.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

import br.com.fiap.postech.soat16.billing.shared.domain.exception.ErrorType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponseDto(
        int status,
        ErrorType type,
        String code,
        String message,
        String path,
        String traceId,
        Instant timestamp
) {

    public static ApiErrorResponseDto of(
            int status, ErrorType type, String code, String message, String path, String traceId) {
        return new ApiErrorResponseDto(status, type, code, message, path, traceId, Instant.now());
    }
}
