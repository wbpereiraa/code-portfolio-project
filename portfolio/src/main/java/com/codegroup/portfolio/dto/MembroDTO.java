package com.codegroup.portfolio.dto;

import com.codegroup.portfolio.enums.AtribuicaoMembro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO usado para criar/consultar membros na API externa mockada.
 */
public record MembroDTO(
        Long id,

        @NotBlank(message = "Nome é obrigatório")
        String nome,

        @NotNull(message = "Atribuição (cargo) é obrigatória")
        AtribuicaoMembro atribuicao
) {
}
