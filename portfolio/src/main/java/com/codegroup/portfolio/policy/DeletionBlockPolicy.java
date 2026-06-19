package com.codegroup.portfolio.policy;

import com.codegroup.portfolio.enums.StatusProjeto;
import com.codegroup.portfolio.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DeletionBlockPolicy {

    private static final Set<StatusProjeto> STATUS_IMPEDEM_EXCLUSAO = Set.of(
            StatusProjeto.INICIADO, StatusProjeto.EM_ANDAMENTO, StatusProjeto.ENCERRADO
    );

    public void validate(StatusProjeto status) {
        if (STATUS_IMPEDEM_EXCLUSAO.contains(status)) {
            throw new BusinessException(
                    "Projeto com status " + status + " não pode ser excluído."
            );
        }
    }
}
