package com.codegroup.portfolio.enums;

import java.util.List;

/**
 * Status fixos do ciclo de vida do projeto.
 * A ordem dos valores representa a sequência lógica permitida
 * (exceto CANCELADO, que pode ser aplicado a qualquer momento).
 */
public enum StatusProjeto {
    EM_ANALISE,
    ANALISE_REALIZADA,
    ANALISE_APROVADA,
    INICIADO,
    PLANEJADO,
    EM_ANDAMENTO,
    ENCERRADO,
    CANCELADO;

    /**
     * Verifica se a transição do status atual para o status destino é permitida.
     * Regras:
     *  - CANCELADO pode ser aplicado a partir de qualquer status (exceto a partir de ENCERRADO ou CANCELADO).
     *  - Demais transições devem seguir a sequência lógica (avanço de exatamente uma etapa) ou
     *    permanecer no mesmo status (sem-op não é considerado uma transição válida de update).
     */
    public boolean podeTransicionarPara(StatusProjeto destino) {
        if (this == ENCERRADO || this == CANCELADO) {
            return false; // status finais, sem novas transições
        }

        if (destino == CANCELADO) {
            return true; // cancelamento permitido a qualquer momento (exceto estados finais acima)
        }

        List<StatusProjeto> sequencia = sequenciaLogica();
        int posicaoAtual = sequencia.indexOf(this);
        int posicaoDestino = sequencia.indexOf(destino);

        if (posicaoAtual == -1 || posicaoDestino == -1) {
            return false;
        }

        // só permite avançar exatamente uma etapa na sequência
        return posicaoDestino == posicaoAtual + 1;
    }

    private static List<StatusProjeto> sequenciaLogica() {
        return List.of(
                EM_ANALISE,
                ANALISE_REALIZADA,
                ANALISE_APROVADA,
                INICIADO,
                PLANEJADO,
                EM_ANDAMENTO,
                ENCERRADO
        );
    }
}
