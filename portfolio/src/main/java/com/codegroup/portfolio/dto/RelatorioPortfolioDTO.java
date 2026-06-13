package com.codegroup.portfolio.dto;

import com.codegroup.portfolio.enums.StatusProjeto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO com o relatório resumido do portfólio.
 */
public record RelatorioPortfolioDTO(
        Map<StatusProjeto, Long> quantidadeProjetosPorStatus,
        Map<StatusProjeto, BigDecimal> totalOrcadoPorStatus,
        Double mediaDuracaoProjetosEncerradosEmDias,
        Long totalMembrosUnicosAlocados
) {
}
