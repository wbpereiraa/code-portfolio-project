package com.codegroup.portfolio.policy;

import com.codegroup.portfolio.entity.Membro;
import com.codegroup.portfolio.enums.AtribuicaoMembro;
import com.codegroup.portfolio.exception.BusinessException;
import com.codegroup.portfolio.repository.ProjetoRepository;
import com.codegroup.portfolio.service.MembroService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberAllocationValidatorTest {

    @Mock private MembroService membroService;
    @Mock private ProjetoRepository projetoRepository;

    private MemberAllocationValidator validator;
    private Membro funcionario;

    @BeforeEach
    void setUp() {
        validator = new MemberAllocationValidator(membroService, projetoRepository);
        funcionario = new Membro(2L, "Bruno Lima", AtribuicaoMembro.FUNCIONARIO);
    }

    @Test
    void deveResolverMembrosComSucesso() {
        when(membroService.obterFuncionarioParaAssociacao(2L)).thenReturn(funcionario);
        when(projetoRepository.countProjetosAtivosPorMembro(eq(2L), isNull())).thenReturn(0L);

        List<Membro> resultado = validator.validateAndResolve(List.of(2L), null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNome()).isEqualTo("Bruno Lima");
    }

    @Test
    void deveLancarExcecaoParaListaVazia() {
        assertThatThrownBy(() -> validator.validateAndResolve(List.of(), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("entre 1 e 10 membros");
    }

    @Test
    void deveLancarExcecaoParaListaNula() {
        assertThatThrownBy(() -> validator.validateAndResolve(null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("entre 1 e 10 membros");
    }

    @Test
    void deveLancarExcecaoParaIdsDuplicados() {
        assertThatThrownBy(() -> validator.validateAndResolve(List.of(2L, 2L), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("duplicados");
    }

    @Test
    void deveLancarExcecaoQuandoMembroTemMaisQue3ProjetosAtivos() {
        when(membroService.obterFuncionarioParaAssociacao(2L)).thenReturn(funcionario);
        when(projetoRepository.countProjetosAtivosPorMembro(eq(2L), isNull())).thenReturn(3L);

        assertThatThrownBy(() -> validator.validateAndResolve(List.of(2L), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("já está alocado");
    }

    @Test
    void deveValidarDatasComSucesso() {
        assertThatCode(() -> validator.validateDates(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 1),
                null
        )).doesNotThrowAnyException();
    }

    @Test
    void deveLancarExcecaoQuandoPrevisaoTerminoAnteriorADataInicio() {
        assertThatThrownBy(() -> validator.validateDates(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 1, 1),
                null
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("previsão de término");
    }

    @Test
    void deveLancarExcecaoQuandoDataRealTerminoAnteriorADataInicio() {
        assertThatThrownBy(() -> validator.validateDates(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 1, 1)
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("data real de término");
    }

    @Test
    void deveLancarExcecaoComMaisDe10Membros() {
        List<Long> onzeIds = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L);

        assertThatThrownBy(() -> validator.validateAndResolve(onzeIds, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("entre 1 e 10 membros");
    }
}
