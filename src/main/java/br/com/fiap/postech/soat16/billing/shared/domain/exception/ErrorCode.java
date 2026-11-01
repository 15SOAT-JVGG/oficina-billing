package br.com.fiap.postech.soat16.billing.shared.domain.exception;

@FunctionalInterface
public interface ErrorCode {

    String getCode();
}
