package com.codegroup.portfolio.service;

import com.codegroup.portfolio.dto.AtualizarStatusDTO;
import com.codegroup.portfolio.dto.ProjetoRequestDTO;
import com.codegroup.portfolio.entity.Membro;
import com.codegroup.portfolio.entity.Projeto;
import com.codegroup.portfolio.enums.AtribuicaoMembro;
import com.codegroup.portfolio.enums.ClassificacaoRisco;
import com.codegroup.portfolio.enums.StatusProjeto;
import com.codegroup.portfolio.exception.BusinessException;
import com.codegroup.portfolio.exception.ResourceNotFoundException;
import com.codegroup.portfolio.repository.ProjetoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjetoServiceTest {

    @Mock
    private ProjetoRepository projetoRepository;

    @Mock
    private MembroService membroService;

    private ClassificacaoRiscoService classificacaoRiscoService;

    @InjectMocks
    private ProjetoService projetoService;

    private Membro gerente;
    private Membro funcionario1;

    @BeforeEach
    void setUp() {
        classificacaoRiscoService = new ClassificacaoRiscoService();
        projetoService = new ProjetoService(projetoRepository, membroService, classificacaoRiscoService);

        gerente = Membro.builder().id(1L).nome("Ana Souza").atribuicao(AtribuicaoMembro.GERENTE).build();
        funcionario1 = Membro.builder().id(2L).nome("Bruno Lima").atribuicao(AtribuicaoMembro.FUNCIONARIO).build();
    }

    private ProjetoRequestDTO criarRequestValido() {
        return new ProjetoRequestDTO(
                "Sistema de Vendas",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 4, 1), // 3 meses -> baixo risco se orcamento <= 100000
                null,
                new BigDecimal("80000.00"),
                "Descrição do projeto",
                gerente.getId(),
                null,
                List.of(funcionario1.getId())
        );
    }

    @Test
    void deveCriarProjetoComStatusInicialEmAnalise() {
        when(membroService.obterMembroSincronizado(gerente.getId())).thenReturn(gerente);
        when(membroService.obterFuncionarioParaAssociacao(funcionario1.getId())).thenReturn(funcionario1);
        when(projetoRepository.countProjetosAtivosPorMembro(eq(funcionario1.getId()), isNull())).thenReturn(0L);
        when(projetoRepository.save(any(Projeto.class))).thenAnswer(invocation -> {
            Projeto p = invocation.getArgument(0);
            p.setId(10L);
            return p;
        });

        var response = projetoService.criar(criarRequestValido());

        assertThat(response.status()).isEqualTo(StatusProjeto.EM_ANALISE);
        assertThat(response.classificacaoRisco()).isEqualTo(ClassificacaoRisco.BAIXO);
        assertThat(response.membros()).hasSize(1);
        verify(projetoRepository).save(any(Projeto.class));
    }

    @Test
    void deveLancarExcecaoQuandoPrevisaoTerminoAnteriorADataInicio() {
        ProjetoRequestDTO dto = new ProjetoRequestDTO(
                "Projeto Inválido",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 1, 1), // antes do início
                null,
                new BigDecimal("10000.00"),
                "desc",
                gerente.getId(),
                null,
                List.of(funcionario1.getId())
        );

        assertThatThrownBy(() -> projetoService.criar(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("previsão de término");

        verify(projetoRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoMembroNaoForFuncionario() {
        when(membroService.obterMembroSincronizado(gerente.getId())).thenReturn(gerente);
        when(membroService.obterFuncionarioParaAssociacao(funcionario1.getId()))
                .thenThrow(new BusinessException("Apenas membros com atribuição FUNCIONARIO podem ser associados a projetos."));

        assertThatThrownBy(() -> projetoService.criar(criarRequestValido()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("FUNCIONARIO");
    }

    @Test
    void deveLancarExcecaoQuandoMembroJaAlocadoEm3ProjetosAtivos() {
        when(membroService.obterMembroSincronizado(gerente.getId())).thenReturn(gerente);
        when(membroService.obterFuncionarioParaAssociacao(funcionario1.getId())).thenReturn(funcionario1);
        when(projetoRepository.countProjetosAtivosPorMembro(eq(funcionario1.getId()), isNull())).thenReturn(3L);

        assertThatThrownBy(() -> projetoService.criar(criarRequestValido()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("já está alocado");

        verify(projetoRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoQuantidadeDeMembrosForaDoLimite() {
        ProjetoRequestDTO dto = new ProjetoRequestDTO(
                "Projeto sem membros",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 4, 1),
                null,
                new BigDecimal("10000.00"),
                "desc",
                gerente.getId(),
                null,
                List.of() // vazio - viola minimo de 1
        );

        assertThatThrownBy(() -> projetoService.criar(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("entre 1 e 10 membros");
    }

    @Test
    void deveImpedirExclusaoDeProjetoComStatusEmAndamento() {
        Projeto projeto = Projeto.builder()
                .id(5L)
                .nome("Projeto X")
                .dataInicio(LocalDate.of(2026, 1, 1))
                .previsaoTermino(LocalDate.of(2026, 4, 1))
                .orcamentoTotal(new BigDecimal("10000.00"))
                .status(StatusProjeto.EM_ANDAMENTO)
                .membros(List.of(funcionario1))
                .build();

        when(projetoRepository.findById(5L)).thenReturn(Optional.of(projeto));

        assertThatThrownBy(() -> projetoService.deletar(5L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não pode ser excluído");

        verify(projetoRepository, never()).delete((Projeto) any());
    }

    @Test
    void devePermitirExclusaoDeProjetoComStatusEmAnalise() {
        Projeto projeto = Projeto.builder()
                .id(6L)
                .nome("Projeto Y")
                .dataInicio(LocalDate.of(2026, 1, 1))
                .previsaoTermino(LocalDate.of(2026, 4, 1))
                .orcamentoTotal(new BigDecimal("10000.00"))
                .status(StatusProjeto.EM_ANALISE)
                .membros(List.of(funcionario1))
                .build();

        when(projetoRepository.findById(6L)).thenReturn(Optional.of(projeto));

        projetoService.deletar(6L);

        verify(projetoRepository).delete(projeto);
    }

    @Test
    void deveLancarExcecaoAoBuscarProjetoInexistente() {
        when(projetoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projetoService.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deveAtualizarStatusQuandoTransicaoValida() {
        Projeto projeto = Projeto.builder()
                .id(7L)
                .nome("Projeto Z")
                .dataInicio(LocalDate.of(2026, 1, 1))
                .previsaoTermino(LocalDate.of(2026, 4, 1))
                .orcamentoTotal(new BigDecimal("10000.00"))
                .status(StatusProjeto.EM_ANALISE)
                .membros(List.of(funcionario1))
                .build();

        when(projetoRepository.findById(7L)).thenReturn(Optional.of(projeto));
        when(projetoRepository.save(any(Projeto.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = projetoService.atualizarStatus(7L, new AtualizarStatusDTO(StatusProjeto.ANALISE_REALIZADA));

        assertThat(response.status()).isEqualTo(StatusProjeto.ANALISE_REALIZADA);
    }

    @Test
    void deveLancarExcecaoAoPularEtapaDeStatus() {
        Projeto projeto = Projeto.builder()
                .id(8L)
                .nome("Projeto W")
                .dataInicio(LocalDate.of(2026, 1, 1))
                .previsaoTermino(LocalDate.of(2026, 4, 1))
                .orcamentoTotal(new BigDecimal("10000.00"))
                .status(StatusProjeto.EM_ANALISE)
                .membros(List.of(funcionario1))
                .build();

        when(projetoRepository.findById(8L)).thenReturn(Optional.of(projeto));

        assertThatThrownBy(() -> projetoService.atualizarStatus(8L, new AtualizarStatusDTO(StatusProjeto.INICIADO)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Transição de status inválida");

        verify(projetoRepository, never()).save(any());
    }

    @Test
    void deveCancelarProjetoAQualquerMomento() {
        Projeto projeto = Projeto.builder()
                .id(9L)
                .nome("Projeto Cancelável")
                .dataInicio(LocalDate.of(2026, 1, 1))
                .previsaoTermino(LocalDate.of(2026, 4, 1))
                .orcamentoTotal(new BigDecimal("10000.00"))
                .status(StatusProjeto.PLANEJADO)
                .membros(List.of(funcionario1))
                .build();

        when(projetoRepository.findById(9L)).thenReturn(Optional.of(projeto));
        when(projetoRepository.save(any(Projeto.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = projetoService.atualizarStatus(9L, new AtualizarStatusDTO(StatusProjeto.CANCELADO));

        assertThat(response.status()).isEqualTo(StatusProjeto.CANCELADO);
    }
}
