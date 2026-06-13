package com.codegroup.portfolio.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ProjetoDTO(
        Long id,
        @NotBlank(message = "Nome é obrigatório")
        String nome,
        String descricao,
        List<Long> membrosIds
) {
}
