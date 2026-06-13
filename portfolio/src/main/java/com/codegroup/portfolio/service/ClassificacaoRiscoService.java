package com.codegroup.portfolio.service;

import com.codegroup.portfolio.enums.ClassificacaoRisco;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Serviço responsável por calcular dinamicamente a classificação de risco
 * de um projeto com base em orçamento e prazo (em meses).
 *
 * Regras:
 *  - BAIXO:  orçamento <= 100.000 E prazo <= 3 meses
 *  - MEDIO:  orçamento entre 100.000,01 e 500.000 OU prazo entre 3 e 6 meses (exclusive nos extremos já cobertos)
 *  - ALTO:   orçamento > 500.000 OU prazo > 6 meses
 */
@Service
public class ClassificacaoRiscoService {

    private static final BigDecimal LIMITE_BAIXO = new BigDecimal("100000.00");
    private static final BigDecimal LIMITE_MEDIO = new BigDecimal("500000.00");

    private static final long PRAZO_BAIXO_MESES = 3;
    private static final long PRAZO_MEDIO_MESES = 6;

    public ClassificacaoRisco calcular(BigDecimal orcamentoTotal, LocalDate dataInicio, LocalDate previsaoTermino) {
        long prazoEmMeses = calcularPrazoEmMeses(dataInicio, previsaoTermino);

        boolean orcamentoAlto = orcamentoTotal.compareTo(LIMITE_MEDIO) > 0;
        boolean prazoAlto = prazoEmMeses > PRAZO_MEDIO_MESES;

        if (orcamentoAlto || prazoAlto) {
            return ClassificacaoRisco.ALTO;
        }

        boolean orcamentoBaixo = orcamentoTotal.compareTo(LIMITE_BAIXO) <= 0;
        boolean prazoBaixo = prazoEmMeses <= PRAZO_BAIXO_MESES;

        if (orcamentoBaixo && prazoBaixo) {
            return ClassificacaoRisco.BAIXO;
        }

        return ClassificacaoRisco.MEDIO;
    }

    private long calcularPrazoEmMeses(LocalDate dataInicio, LocalDate previsaoTermino) {
        return ChronoUnit.MONTHS.between(dataInicio, previsaoTermino);
    }
}
