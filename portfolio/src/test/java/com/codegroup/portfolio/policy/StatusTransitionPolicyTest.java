package com.codegroup.portfolio.policy;

import com.codegroup.portfolio.enums.StatusProjeto;
import com.codegroup.portfolio.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatusTransitionPolicyTest {

    private final StatusTransitionPolicy policy = new StatusTransitionPolicy();

    @ParameterizedTest
    @CsvSource({
            "EM_ANALISE, ANALISE_REALIZADA",
            "ANALISE_REALIZADA, ANALISE_APROVADA",
            "ANALISE_APROVADA, INICIADO",
            "INICIADO, PLANEJADO",
            "PLANEJADO, EM_ANDAMENTO",
            "EM_ANDAMENTO, ENCERRADO"
    })
    void devePermitirTransicaoSequencialValida(StatusProjeto atual, StatusProjeto destino) {
        assertThatCode(() -> policy.validate(atual, destino)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
            "EM_ANALISE, CANCELADO",
            "ANALISE_APROVADA, CANCELADO",
            "EM_ANDAMENTO, CANCELADO"
    })
    void devePermitirCancelamento(StatusProjeto atual, StatusProjeto destino) {
        assertThatCode(() -> policy.validate(atual, destino)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
            "EM_ANALISE, INICIADO",
            "ANALISE_REALIZADA, EM_ANALISE",
            "ENCERRADO, CANCELADO",
            "CANCELADO, EM_ANALISE"
    })
    void deveBloquearTransicaoInvalida(StatusProjeto atual, StatusProjeto destino) {
        assertThatThrownBy(() -> policy.validate(atual, destino))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Transição de status inválida");
    }

    @Test
    void devePermitirMesmoStatus() {
        assertThatCode(() -> policy.validate(StatusProjeto.EM_ANALISE, StatusProjeto.EM_ANALISE))
                .doesNotThrowAnyException();
    }
}
