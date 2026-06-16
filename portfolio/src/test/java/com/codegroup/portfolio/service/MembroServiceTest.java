package com.codegroup.portfolio.service;

import com.codegroup.portfolio.client.MembroExternalClient;
import com.codegroup.portfolio.dto.MembroDTO;
import com.codegroup.portfolio.entity.Membro;
import com.codegroup.portfolio.enums.AtribuicaoMembro;
import com.codegroup.portfolio.exception.BusinessException;
import com.codegroup.portfolio.repository.MembroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembroServiceTest {

    @Mock private MembroExternalClient membroExternalClient;
    @Mock private MembroRepository membroRepository;

    @InjectMocks
    private MembroService membroService;

    private MembroDTO funcionarioDTO;
    private MembroDTO gerenteDTO;

    @BeforeEach
    void setUp() {
        funcionarioDTO = new MembroDTO(2L, "Bruno Lima", AtribuicaoMembro.FUNCIONARIO);
        gerenteDTO    = new MembroDTO(1L, "Ana Souza",  AtribuicaoMembro.GERENTE);
    }

    // -----------------------------------------------------------------------
    // listarTodos / buscarPorId / criar — delegam para o client externo
    // -----------------------------------------------------------------------

    @Test
    void deveListarTodosViaClientExterno() {
        when(membroExternalClient.listarTodos()).thenReturn(List.of(funcionarioDTO, gerenteDTO));

        List<MembroDTO> resultado = membroService.listarTodos();

        assertThat(resultado).hasSize(2);
        verify(membroExternalClient).listarTodos();
    }

    @Test
    void deveBuscarPorIdViaClientExterno() {
        when(membroExternalClient.buscarPorId(2L)).thenReturn(funcionarioDTO);

        MembroDTO resultado = membroService.buscarPorId(2L);

        assertThat(resultado.nome()).isEqualTo("Bruno Lima");
        verify(membroExternalClient).buscarPorId(2L);
    }

    @Test
    void deveCriarViaClientExterno() {
        when(membroExternalClient.criar(funcionarioDTO)).thenReturn(funcionarioDTO);

        MembroDTO resultado = membroService.criar(funcionarioDTO);

        assertThat(resultado).isEqualTo(funcionarioDTO);
        verify(membroExternalClient).criar(funcionarioDTO);
    }

    // -----------------------------------------------------------------------
    // obterFuncionarioParaAssociacao
    // -----------------------------------------------------------------------

    @Test
    void deveObterFuncionarioECriarCacheLocalSeNaoExistir() {
        when(membroExternalClient.buscarPorId(2L)).thenReturn(funcionarioDTO);
        when(membroRepository.findById(2L)).thenReturn(Optional.empty());
        when(membroRepository.save(any(Membro.class))).thenAnswer(inv -> inv.getArgument(0));

        Membro resultado = membroService.obterFuncionarioParaAssociacao(2L);

        assertThat(resultado.getNome()).isEqualTo("Bruno Lima");
        assertThat(resultado.getAtribuicao()).isEqualTo(AtribuicaoMembro.FUNCIONARIO);
        verify(membroRepository).save(any(Membro.class));
    }

    @Test
    void deveAtualizarCacheLocalQuandoMembroJaExisteNoBanco() {
        Membro existente = new Membro(2L, "Bruno Antigo", AtribuicaoMembro.FUNCIONARIO);
        when(membroExternalClient.buscarPorId(2L)).thenReturn(funcionarioDTO);
        when(membroRepository.findById(2L)).thenReturn(Optional.of(existente));
        when(membroRepository.save(existente)).thenReturn(existente);

        Membro resultado = membroService.obterFuncionarioParaAssociacao(2L);

        assertThat(resultado.getNome()).isEqualTo("Bruno Lima"); // atualizado
        verify(membroRepository).save(existente);
    }

    @Test
    void deveLancarExcecaoQuandoMembroNaoForFuncionario() {
        when(membroExternalClient.buscarPorId(1L)).thenReturn(gerenteDTO);

        assertThatThrownBy(() -> membroService.obterFuncionarioParaAssociacao(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("FUNCIONARIO")
                .hasMessageContaining("Ana Souza");

        verify(membroRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // obterMembroSincronizado (sem restrição de atribuição)
    // -----------------------------------------------------------------------

    @Test
    void deveObterGerenteParaSincronizacaoSemValidarAtribuicao() {
        when(membroExternalClient.buscarPorId(1L)).thenReturn(gerenteDTO);
        when(membroRepository.findById(1L)).thenReturn(Optional.empty());
        when(membroRepository.save(any(Membro.class))).thenAnswer(inv -> inv.getArgument(0));

        Membro resultado = membroService.obterMembroSincronizado(1L);

        assertThat(resultado.getAtribuicao()).isEqualTo(AtribuicaoMembro.GERENTE);
        verify(membroRepository).save(any(Membro.class));
    }

    @Test
    void deveObterFuncionarioParaSincronizacaoSemValidarAtribuicao() {
        when(membroExternalClient.buscarPorId(2L)).thenReturn(funcionarioDTO);
        when(membroRepository.findById(2L)).thenReturn(Optional.empty());
        when(membroRepository.save(any(Membro.class))).thenAnswer(inv -> inv.getArgument(0));

        Membro resultado = membroService.obterMembroSincronizado(2L);

        assertThat(resultado.getNome()).isEqualTo("Bruno Lima");
    }
}
