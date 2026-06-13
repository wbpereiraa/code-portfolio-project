package com.codegroup.portfolio.dto;

import com.codegroup.portfolio.enums.StatusProjeto;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO de entrada para criação/atualização de Projeto.
 */
public record ProjetoRequestDTO(

        @NotBlank(message = "Nome é obrigatório")
        String nome,

        @NotNull(message = "Data de início é obrigatória")
        LocalDate dataInicio,

        @NotNull(message = "Previsão de término é obrigatória")
        LocalDate previsaoTermino,

        LocalDate dataRealTermino,

        @NotNull(message = "Orçamento total é obrigatório")
        @DecimalMin(value = "0.0", inclusive = true, message = "Orçamento não pode ser negativo")
        BigDecimal orcamentoTotal,

        @Size(max = 2000, message = "Descrição deve ter no máximo 2000 caracteres")
        String descricao,

        @NotNull(message = "Gerente responsável é obrigatório")
        Long gerenteResponsavelId,

        StatusProjeto status,

        @NotNull(message = "É necessário informar ao menos 1 membro")
        @Size(min = 1, max = 10, message = "O projeto deve ter entre 1 e 10 membros")
        List<Long> membrosIds
) {
}
