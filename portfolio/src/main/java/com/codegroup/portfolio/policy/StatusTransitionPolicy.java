package com.codegroup.portfolio.policy;

import com.codegroup.portfolio.enums.StatusProjeto;
import com.codegroup.portfolio.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class StatusTransitionPolicy {

    public void validate(StatusProjeto atual, StatusProjeto destino) {
        if (atual == destino) {
            return;
        }

        if (!atual.podeTransicionarPara(destino)) {
            throw new BusinessException(
                    "Transição de status inválida: não é possível ir de " + atual + " para " + destino + "."
            );
        }
    }
}
