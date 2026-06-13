package com.codegroup.portfolio.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ClassificacaoRiscoServiceTest {

    private final ClassificacaoRiscoService service = new ClassificacaoRiscoService();

    @Test
    void deveClassificarComoBaixoQuandoOrcamentoEPrazoDentroDoLimite() {
        var risco = service.calcular(new BigDecimal("100000.00"),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 1)); // 3 meses
        assertThat(risco).isEqualTo(com.codegroup.portfolio.enums.ClassificacaoRisco.BAIXO);
    }

    @Test
    void deveClassificarComoMedioQuandoOrcamentoEntre100001E500000() {
        var risco = service.calcular(new BigDecimal("250000.00"),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 1)); // 3 meses
        assertThat(risco).isEqualTo(com.codegroup.portfolio.enums.ClassificacaoRisco.MEDIO);
    }

    @Test
    void deveClassificarComoMedioQuandoPrazoEntre3E6Meses() {
        var risco = service.calcular(new BigDecimal("50000.00"),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 1)); // 5 meses
        assertThat(risco).isEqualTo(com.codegroup.portfolio.enums.ClassificacaoRisco.MEDIO);
    }

    @Test
    void deveClassificarComoAltoQuandoOrcamentoAcimaDe500000() {
        var risco = service.calcular(new BigDecimal("600000.00"),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1)); // 1 mes
        assertThat(risco).isEqualTo(com.codegroup.portfolio.enums.ClassificacaoRisco.ALTO);
    }

    @Test
    void deveClassificarComoAltoQuandoPrazoSuperiorA6Meses() {
        var risco = service.calcular(new BigDecimal("10000.00"),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 9, 1)); // 8 meses
        assertThat(risco).isEqualTo(com.codegroup.portfolio.enums.ClassificacaoRisco.ALTO);
    }

    @ParameterizedTest
    @CsvSource({
            "100000.00, 3, BAIXO",
            "100001.00, 3, MEDIO",
            "500000.00, 6, MEDIO",
            "500001.00, 6, ALTO"
    })
    void deveRespeitarLimitesExatos(String orcamento, int meses, String classificacaoEsperada) {
        LocalDate inicio = LocalDate.of(2026, 1, 1);
        LocalDate fim = inicio.plusMonths(meses);

        var risco = service.calcular(new BigDecimal(orcamento), inicio, fim);

        assertThat(risco.name()).isEqualTo(classificacaoEsperada);
    }
}
