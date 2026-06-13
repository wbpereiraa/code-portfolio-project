package com.codegroup.portfolio.dto;

import com.codegroup.portfolio.enums.ClassificacaoRisco;
import com.codegroup.portfolio.enums.StatusProjeto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO de saída representando um Projeto, incluindo a classificação de
 * risco calculada dinamicamente.
 */
public record ProjetoResponseDTO(
        Long id,
        String nome,
        LocalDate dataInicio,
        LocalDate previsaoTermino,
        LocalDate dataRealTermino,
        BigDecimal orcamentoTotal,
        String descricao,
        MembroDTO gerenteResponsavel,
        StatusProjeto status,
        ClassificacaoRisco classificacaoRisco,
        List<MembroDTO> membros
) {
}
