package com.codegroup.portfolio.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class StatusProjetoTest {

    @ParameterizedTest
    @CsvSource({
            "EM_ANALISE, ANALISE_REALIZADA, true",
            "ANALISE_REALIZADA, ANALISE_APROVADA, true",
            "ANALISE_APROVADA, INICIADO, true",
            "INICIADO, PLANEJADO, true",
            "PLANEJADO, EM_ANDAMENTO, true",
            "EM_ANDAMENTO, ENCERRADO, true",
            "EM_ANALISE, ANALISE_APROVADA, false",
            "EM_ANALISE, INICIADO, false",
            "ANALISE_REALIZADA, EM_ANALISE, false",
            "ENCERRADO, EM_ANDAMENTO, false",
            "ENCERRADO, CANCELADO, false"
    })
    void deveValidarTransicoesDeStatus(StatusProjeto atual, StatusProjeto destino, boolean esperado) {
        assertThat(atual.podeTransicionarPara(destino)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @CsvSource({
            "EM_ANALISE, CANCELADO, true",
            "ANALISE_APROVADA, CANCELADO, true",
            "EM_ANDAMENTO, CANCELADO, true"
    })
    void devePermitirCancelamentoAQualquerMomentoExcetoStatusFinais(StatusProjeto atual, StatusProjeto destino, boolean esperado) {
        assertThat(atual.podeTransicionarPara(destino)).isEqualTo(esperado);
    }

    @Test
    void cancelarUmProjetoCanceladoNaoDeveSerPermitido() {
        assertThat(StatusProjeto.CANCELADO.podeTransicionarPara(StatusProjeto.EM_ANDAMENTO)).isFalse();
    }
}
