package com.codegroup.portfolio.service;

import com.codegroup.portfolio.dto.AtualizarStatusDTO;
import com.codegroup.portfolio.dto.ProjetoRequestDTO;
import com.codegroup.portfolio.dto.ProjetoResponseDTO;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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

    @Mock private ProjetoRepository projetoRepository;
    @Mock private MembroService membroService;

    private ClassificacaoRiscoService classificacaoRiscoService;

    @InjectMocks
    private ProjetoService projetoService;

    private Membro gerente;
    private Membro funcionario1;
    private Membro funcionario2;

    @BeforeEach
    void setUp() {
        classificacaoRiscoService = new ClassificacaoRiscoService();
        projetoService = new ProjetoService(projetoRepository, membroService, classificacaoRiscoService);

        gerente      = new Membro(1L, "Ana Souza",   AtribuicaoMembro.GERENTE);
        funcionario1 = new Membro(2L, "Bruno Lima",  AtribuicaoMembro.FUNCIONARIO);
        funcionario2 = new Membro(3L, "Carla Mendes",AtribuicaoMembro.FUNCIONARIO);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private ProjetoRequestDTO requestValido(List<Long> membrosIds) {
        return new ProjetoRequestDTO(
                "Sistema de Vendas",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 4, 1),   // 3 meses → BAIXO (orcamento <= 100k)
                null,
                new BigDecimal("80000.00"),
                "Descrição",
                gerente.getId(),
                null,
                membrosIds
        );
    }

    private Projeto projetoComStatus(Long id, StatusProjeto status) {
        return Projeto.builder()
                .id(id)
                .nome("Projeto " + id)
                .dataInicio(LocalDate.of(2026, 1, 1))
                .previsaoTermino(LocalDate.of(2026, 4, 1))
                .orcamentoTotal(new BigDecimal("80000.00"))
                .status(status)
                .membros(List.of(funcionario1))
                .build();
    }

    // -----------------------------------------------------------------------
    // criar
    // -----------------------------------------------------------------------

    @Test
    void deveCriarProjetoComStatusInicialEmAnalise() {
        when(membroService.obterMembroSincronizado(gerente.getId())).thenReturn(gerente);
        when(membroService.obterFuncionarioParaAssociacao(funcionario1.getId())).thenReturn(funcionario1);
        when(projetoRepository.countProjetosAtivosPorMembro(eq(funcionario1.getId()), isNull())).thenReturn(0L);
        when(projetoRepository.save(any(Projeto.class))).thenAnswer(inv -> {
            Projeto p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });

        ProjetoResponseDTO response = projetoService.criar(requestValido(List.of(funcionario1.getId())));

        assertThat(response.status()).isEqualTo(StatusProjeto.EM_ANALISE);
        assertThat(response.classificacaoRisco()).isEqualTo(ClassificacaoRisco.BAIXO);
        assertThat(response.membros()).hasSize(1);
        verify(projetoRepository).save(any(Projeto.class));
    }

    @Test
    void deveCriarProjetoComClassificacaoAlto() {
        ProjetoRequestDTO dto = new ProjetoRequestDTO(
                "Big Project",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 10, 1), // 9 meses → ALTO
                null,
                new BigDecimal("600000.00"),
                "desc",
                gerente.getId(),
                null,
                List.of(funcionario1.getId())
        );

        when(membroService.obterMembroSincronizado(gerente.getId())).thenReturn(gerente);
        when(membroService.obterFuncionarioParaAssociacao(funcionario1.getId())).thenReturn(funcionario1);
        when(projetoRepository.countProjetosAtivosPorMembro(eq(funcionario1.getId()), isNull())).thenReturn(0L);
        when(projetoRepository.save(any())).thenAnswer(inv -> { Projeto p = inv.getArgument(0); p.setId(1L); return p; });

        ProjetoResponseDTO response = projetoService.criar(dto);

        assertThat(response.classificacaoRisco()).isEqualTo(ClassificacaoRisco.ALTO);
    }

    @Test
    void deveLancarExcecaoQuandoPrevisaoTerminoAnteriorADataInicio() {
        ProjetoRequestDTO dto = new ProjetoRequestDTO(
                "Inválido",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 1, 1), // antes do início
                null, new BigDecimal("10000"), "desc",
                gerente.getId(), null, List.of(funcionario1.getId())
        );

        assertThatThrownBy(() -> projetoService.criar(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("previsão de término");

        verify(projetoRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoDataRealTerminoAnteriorADataInicio() {
        ProjetoRequestDTO dto = new ProjetoRequestDTO(
                "Inválido",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 1, 1), // data real antes do início
                new BigDecimal("10000"), "desc",
                gerente.getId(), null, List.of(funcionario1.getId())
        );

        assertThatThrownBy(() -> projetoService.criar(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("data real de término");

        verify(projetoRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoComListaDeMembrosVazia() {
        assertThatThrownBy(() -> projetoService.criar(requestValido(List.of())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("entre 1 e 10 membros");
    }

    @Test
    void deveLancarExcecaoComListaDeMembrosNula() {
        assertThatThrownBy(() -> projetoService.criar(requestValido(null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("entre 1 e 10 membros");
    }

    @Test
    void deveLancarExcecaoComMaisDeDezmembros() {
        List<Long> onzeIds = List.of(1L,2L,3L,4L,5L,6L,7L,8L,9L,10L,11L);

        assertThatThrownBy(() -> projetoService.criar(requestValido(onzeIds)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("entre 1 e 10 membros");
    }

    @Test
    void deveLancarExcecaoComIdsDeMemburosDuplicados() {
        when(membroService.obterMembroSincronizado(gerente.getId())).thenReturn(gerente);

        assertThatThrownBy(() -> projetoService.criar(requestValido(List.of(2L, 2L))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("duplicados");
    }

    @Test
    void deveLancarExcecaoQuandoMembroNaoEFuncionario() {
        when(membroService.obterMembroSincronizado(gerente.getId())).thenReturn(gerente);
        when(membroService.obterFuncionarioParaAssociacao(funcionario1.getId()))
                .thenThrow(new BusinessException("Apenas membros com atribuição FUNCIONARIO podem ser associados."));

        assertThatThrownBy(() -> projetoService.criar(requestValido(List.of(funcionario1.getId()))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("FUNCIONARIO");

        verify(projetoRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoMembroJaEstaEm3ProjetosAtivos() {
        when(membroService.obterMembroSincronizado(gerente.getId())).thenReturn(gerente);
        when(membroService.obterFuncionarioParaAssociacao(funcionario1.getId())).thenReturn(funcionario1);
        when(projetoRepository.countProjetosAtivosPorMembro(eq(funcionario1.getId()), isNull())).thenReturn(3L);

        assertThatThrownBy(() -> projetoService.criar(requestValido(List.of(funcionario1.getId()))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("já está alocado");

        verify(projetoRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // buscarPorId
    // -----------------------------------------------------------------------

    @Test
    void deveBuscarProjetoPorId() {
        Projeto projeto = projetoComStatus(5L, StatusProjeto.EM_ANALISE);
        when(projetoRepository.findById(5L)).thenReturn(Optional.of(projeto));

        ProjetoResponseDTO response = projetoService.buscarPorId(5L);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.status()).isEqualTo(StatusProjeto.EM_ANALISE);
    }

    @Test
    void deveLancarExcecaoAoBuscarProjetoInexistente() {
        when(projetoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projetoService.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // -----------------------------------------------------------------------
    // atualizar
    // -----------------------------------------------------------------------

    @Test
    void deveAtualizarDadosDoProjetoComSucesso() {
        Projeto projeto = projetoComStatus(7L, StatusProjeto.EM_ANALISE);
        when(projetoRepository.findById(7L)).thenReturn(Optional.of(projeto));
        when(membroService.obterMembroSincronizado(gerente.getId())).thenReturn(gerente);
        when(membroService.obterFuncionarioParaAssociacao(funcionario1.getId())).thenReturn(funcionario1);
        when(projetoRepository.countProjetosAtivosPorMembro(eq(funcionario1.getId()), eq(7L))).thenReturn(0L);
        when(projetoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProjetoResponseDTO response = projetoService.atualizar(7L, requestValido(List.of(funcionario1.getId())));

        assertThat(response.nome()).isEqualTo("Sistema de Vendas");
        verify(projetoRepository).save(any(Projeto.class));
    }

    @Test
    void deveAtualizarStatusJuntoComDadosSeStatusDiferente() {
        Projeto projeto = projetoComStatus(7L, StatusProjeto.EM_ANALISE);
        when(projetoRepository.findById(7L)).thenReturn(Optional.of(projeto));
        when(membroService.obterMembroSincronizado(gerente.getId())).thenReturn(gerente);
        when(membroService.obterFuncionarioParaAssociacao(funcionario1.getId())).thenReturn(funcionario1);
        when(projetoRepository.countProjetosAtivosPorMembro(eq(funcionario1.getId()), eq(7L))).thenReturn(0L);
        when(projetoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProjetoRequestDTO dtoComStatus = new ProjetoRequestDTO(
                "Atualizado",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 1),
                null, new BigDecimal("80000"), "desc",
                gerente.getId(), StatusProjeto.ANALISE_REALIZADA,
                List.of(funcionario1.getId())
        );

        ProjetoResponseDTO response = projetoService.atualizar(7L, dtoComStatus);

        assertThat(response.status()).isEqualTo(StatusProjeto.ANALISE_REALIZADA);
    }

    @Test
    void deveLancarExcecaoAoAtualizarComTransicaoDeStatusInvalida() {
        Projeto projeto = projetoComStatus(7L, StatusProjeto.EM_ANALISE);
        when(projetoRepository.findById(7L)).thenReturn(Optional.of(projeto));
        when(membroService.obterMembroSincronizado(gerente.getId())).thenReturn(gerente);
        when(membroService.obterFuncionarioParaAssociacao(funcionario1.getId())).thenReturn(funcionario1);
        when(projetoRepository.countProjetosAtivosPorMembro(any(), any())).thenReturn(0L);

        ProjetoRequestDTO dtoStatusInvalido = new ProjetoRequestDTO(
                "Atualizado",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 1),
                null, new BigDecimal("80000"), "desc",
                gerente.getId(), StatusProjeto.ENCERRADO, // pulo inválido
                List.of(funcionario1.getId())
        );

        assertThatThrownBy(() -> projetoService.atualizar(7L, dtoStatusInvalido))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Transição de status inválida");
    }

    // -----------------------------------------------------------------------
    // atualizarStatus
    // -----------------------------------------------------------------------

    @Test
    void deveAtualizarStatusQuandoTransicaoValida() {
        Projeto projeto = projetoComStatus(8L, StatusProjeto.EM_ANALISE);
        when(projetoRepository.findById(8L)).thenReturn(Optional.of(projeto));
        when(projetoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProjetoResponseDTO response = projetoService.atualizarStatus(8L,
                new AtualizarStatusDTO(StatusProjeto.ANALISE_REALIZADA));

        assertThat(response.status()).isEqualTo(StatusProjeto.ANALISE_REALIZADA);
    }

    @Test
    void devePermitirCancelamentoEmQualquerEtapa() {
        for (StatusProjeto status : List.of(
                StatusProjeto.EM_ANALISE, StatusProjeto.ANALISE_REALIZADA,
                StatusProjeto.ANALISE_APROVADA, StatusProjeto.INICIADO,
                StatusProjeto.PLANEJADO, StatusProjeto.EM_ANDAMENTO)) {

            Projeto projeto = projetoComStatus(1L, status);
            when(projetoRepository.findById(1L)).thenReturn(Optional.of(projeto));
            when(projetoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ProjetoResponseDTO response = projetoService.atualizarStatus(1L,
                    new AtualizarStatusDTO(StatusProjeto.CANCELADO));

            assertThat(response.status()).isEqualTo(StatusProjeto.CANCELADO);
        }
    }

    @Test
    void deveLancarExcecaoAoPularEtapaDeStatus() {
        Projeto projeto = projetoComStatus(8L, StatusProjeto.EM_ANALISE);
        when(projetoRepository.findById(8L)).thenReturn(Optional.of(projeto));

        assertThatThrownBy(() -> projetoService.atualizarStatus(8L,
                new AtualizarStatusDTO(StatusProjeto.INICIADO))) // pula 2 etapas
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Transição de status inválida");

        verify(projetoRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoAoTentarRetrocederStatus() {
        Projeto projeto = projetoComStatus(8L, StatusProjeto.ANALISE_APROVADA);
        when(projetoRepository.findById(8L)).thenReturn(Optional.of(projeto));

        assertThatThrownBy(() -> projetoService.atualizarStatus(8L,
                new AtualizarStatusDTO(StatusProjeto.EM_ANALISE)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Transição de status inválida");
    }

    @Test
    void naoDeveLancarExcecaoQuandoStatusIgualAoAtual() {
        // mesmo status = no-op permitido (validarTransicaoStatus faz return)
        Projeto projeto = projetoComStatus(9L, StatusProjeto.EM_ANALISE);
        when(projetoRepository.findById(9L)).thenReturn(Optional.of(projeto));
        when(projetoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // não deve lançar exceção
        ProjetoResponseDTO response = projetoService.atualizarStatus(9L,
                new AtualizarStatusDTO(StatusProjeto.EM_ANALISE));

        assertThat(response.status()).isEqualTo(StatusProjeto.EM_ANALISE);
    }

    // -----------------------------------------------------------------------
    // deletar
    // -----------------------------------------------------------------------

    @Test
    void devePermitirExclusaoDeProjetoComStatusEmAnalise() {
        Projeto projeto = projetoComStatus(6L, StatusProjeto.EM_ANALISE);
        when(projetoRepository.findById(6L)).thenReturn(Optional.of(projeto));

        projetoService.deletar(6L);

        verify(projetoRepository).delete(projeto);
    }

    @Test
    void devePermitirExclusaoDeProjetoComStatusCancelado() {
        Projeto projeto = projetoComStatus(6L, StatusProjeto.CANCELADO);
        when(projetoRepository.findById(6L)).thenReturn(Optional.of(projeto));

        projetoService.deletar(6L);

        verify(projetoRepository).delete(projeto);
    }

    @ParameterizedTest
    @EnumSource(value = StatusProjeto.class, names = {"INICIADO", "EM_ANDAMENTO", "ENCERRADO"})
    void deveImpedirExclusaoParaStatusQueBloqueiam(StatusProjeto status) {
        Projeto projeto = projetoComStatus(5L, status);
        when(projetoRepository.findById(5L)).thenReturn(Optional.of(projeto));

        assertThatThrownBy(() -> projetoService.deletar(5L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não pode ser excluído");

        verify(projetoRepository, never()).delete((Projeto) any());
    }

    @Test
    void deveLancarExcecaoAoDeletarProjetoInexistente() {
        when(projetoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projetoService.deletar(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}