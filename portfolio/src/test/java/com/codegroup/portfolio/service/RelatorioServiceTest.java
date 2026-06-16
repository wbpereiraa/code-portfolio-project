package com.codegroup.portfolio.service;

import com.codegroup.portfolio.dto.RelatorioPortfolioDTO;
import com.codegroup.portfolio.entity.Membro;
import com.codegroup.portfolio.entity.Projeto;
import com.codegroup.portfolio.enums.AtribuicaoMembro;
import com.codegroup.portfolio.enums.StatusProjeto;
import com.codegroup.portfolio.repository.ProjetoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    @Mock
    private ProjetoRepository projetoRepository;

    @InjectMocks
    private RelatorioService relatorioService;

    private Membro membro(Long id, String nome) {
        return new Membro(id, nome, AtribuicaoMembro.FUNCIONARIO);
    }

    private Projeto projeto(Long id, StatusProjeto status, BigDecimal orcamento,
                            LocalDate inicio, LocalDate termino, Membro... membros) {
        return Projeto.builder()
                .id(id)
                .nome("Projeto " + id)
                .status(status)
                .orcamentoTotal(orcamento)
                .dataInicio(inicio)
                .previsaoTermino(termino)
                .dataRealTermino(status == StatusProjeto.ENCERRADO ? termino : null)
                .membros(List.of(membros))
                .build();
    }

    @Test
    void deveRetornarRelatorioComPortfolioVazio() {
        when(projetoRepository.findAll()).thenReturn(List.of());

        RelatorioPortfolioDTO relatorio = relatorioService.gerarRelatorio();

        assertThat(relatorio.totalMembrosUnicosAlocados()).isZero();
        assertThat(relatorio.mediaDuracaoProjetosEncerradosEmDias()).isZero();
        assertThat(relatorio.quantidadeProjetosPorStatus().values())
                .allMatch(v -> v == 0L);
    }

    @Test
    void deveContarProjetosPorStatusCorretamente() {
        Membro m1 = membro(1L, "Ana");
        Projeto p1 = projeto(1L, StatusProjeto.EM_ANALISE,   new BigDecimal("10000"), LocalDate.of(2026,1,1), LocalDate.of(2026,4,1), m1);
        Projeto p2 = projeto(2L, StatusProjeto.EM_ANALISE,   new BigDecimal("20000"), LocalDate.of(2026,1,1), LocalDate.of(2026,4,1), m1);
        Projeto p3 = projeto(3L, StatusProjeto.EM_ANDAMENTO, new BigDecimal("50000"), LocalDate.of(2026,1,1), LocalDate.of(2026,4,1), m1);

        when(projetoRepository.findAll()).thenReturn(List.of(p1, p2, p3));

        RelatorioPortfolioDTO relatorio = relatorioService.gerarRelatorio();

        assertThat(relatorio.quantidadeProjetosPorStatus().get(StatusProjeto.EM_ANALISE)).isEqualTo(2L);
        assertThat(relatorio.quantidadeProjetosPorStatus().get(StatusProjeto.EM_ANDAMENTO)).isEqualTo(1L);
        assertThat(relatorio.quantidadeProjetosPorStatus().get(StatusProjeto.ENCERRADO)).isEqualTo(0L);
    }

    @Test
    void deveSomarOrcamentoPorStatusCorretamente() {
        Membro m1 = membro(1L, "Ana");
        Projeto p1 = projeto(1L, StatusProjeto.EM_ANALISE, new BigDecimal("30000"), LocalDate.of(2026,1,1), LocalDate.of(2026,4,1), m1);
        Projeto p2 = projeto(2L, StatusProjeto.EM_ANALISE, new BigDecimal("70000"), LocalDate.of(2026,1,1), LocalDate.of(2026,4,1), m1);

        when(projetoRepository.findAll()).thenReturn(List.of(p1, p2));

        RelatorioPortfolioDTO relatorio = relatorioService.gerarRelatorio();

        assertThat(relatorio.totalOrcadoPorStatus().get(StatusProjeto.EM_ANALISE))
                .isEqualByComparingTo(new BigDecimal("100000"));
    }

    @Test
    void deveCalcularMediaDuracaoDeProjetosEncerrados() {
        Membro m1 = membro(1L, "Ana");
        // projeto 1: 90 dias; projeto 2: 30 dias → média = 60 dias
        Projeto p1 = Projeto.builder()
                .id(1L).nome("P1").status(StatusProjeto.ENCERRADO)
                .orcamentoTotal(new BigDecimal("10000"))
                .dataInicio(LocalDate.of(2026, 1, 1))
                .previsaoTermino(LocalDate.of(2026, 4, 1))
                .dataRealTermino(LocalDate.of(2026, 4, 1))  // 90 dias
                .membros(List.of(m1)).build();

        Projeto p2 = Projeto.builder()
                .id(2L).nome("P2").status(StatusProjeto.ENCERRADO)
                .orcamentoTotal(new BigDecimal("10000"))
                .dataInicio(LocalDate.of(2026, 1, 1))
                .previsaoTermino(LocalDate.of(2026, 1, 31))
                .dataRealTermino(LocalDate.of(2026, 1, 31)) // 30 dias
                .membros(List.of(m1)).build();

        when(projetoRepository.findAll()).thenReturn(List.of(p1, p2));

        RelatorioPortfolioDTO relatorio = relatorioService.gerarRelatorio();

        assertThat(relatorio.mediaDuracaoProjetosEncerradosEmDias()).isEqualTo(60.0);
    }

    @Test
    void deveIgnorarProjetosEncerradosSemDataRealTermino() {
        Membro m1 = membro(1L, "Ana");
        Projeto semData = Projeto.builder()
                .id(1L).nome("P1").status(StatusProjeto.ENCERRADO)
                .orcamentoTotal(new BigDecimal("10000"))
                .dataInicio(LocalDate.of(2026, 1, 1))
                .previsaoTermino(LocalDate.of(2026, 4, 1))
                .dataRealTermino(null) // sem data real
                .membros(List.of(m1)).build();

        when(projetoRepository.findAll()).thenReturn(List.of(semData));

        RelatorioPortfolioDTO relatorio = relatorioService.gerarRelatorio();

        assertThat(relatorio.mediaDuracaoProjetosEncerradosEmDias()).isZero();
    }

    @Test
    void deveContarMembrosUnicosDesconsiderandoDuplicatas() {
        Membro m1 = membro(1L, "Ana");
        Membro m2 = membro(2L, "Bruno");

        // m1 está em dois projetos — deve ser contado só uma vez
        Projeto p1 = projeto(1L, StatusProjeto.EM_ANDAMENTO, new BigDecimal("10000"),
                LocalDate.of(2026,1,1), LocalDate.of(2026,4,1), m1, m2);
        Projeto p2 = projeto(2L, StatusProjeto.PLANEJADO, new BigDecimal("10000"),
                LocalDate.of(2026,1,1), LocalDate.of(2026,4,1), m1);

        when(projetoRepository.findAll()).thenReturn(List.of(p1, p2));

        RelatorioPortfolioDTO relatorio = relatorioService.gerarRelatorio();

        assertThat(relatorio.totalMembrosUnicosAlocados()).isEqualTo(2L);
    }

    @Test
    void deveRetornarZeroMembrosQuandoProjetosNaoTemMembros() {
        Projeto p1 = projeto(1L, StatusProjeto.EM_ANALISE, new BigDecimal("10000"),
                LocalDate.of(2026,1,1), LocalDate.of(2026,4,1));

        when(projetoRepository.findAll()).thenReturn(List.of(p1));

        RelatorioPortfolioDTO relatorio = relatorioService.gerarRelatorio();

        assertThat(relatorio.totalMembrosUnicosAlocados()).isZero();
    }
}
