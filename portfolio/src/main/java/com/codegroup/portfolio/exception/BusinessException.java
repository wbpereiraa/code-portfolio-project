package com.codegroup.portfolio.exception;

/**
 * Exceção para violações de regras de negócio (ex.: transição de status
 * inválida, limite de membros excedido, exclusão de projeto não permitida, etc.).
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
