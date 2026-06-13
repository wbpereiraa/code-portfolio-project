package com.codegroup.portfolio.dto;

import com.codegroup.portfolio.enums.StatusProjeto;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusDTO(
        @NotNull(message = "Novo status é obrigatório")
        StatusProjeto novoStatus
) {
}
