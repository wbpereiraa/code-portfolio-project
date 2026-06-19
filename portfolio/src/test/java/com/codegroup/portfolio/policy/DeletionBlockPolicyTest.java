package com.codegroup.portfolio.policy;

import com.codegroup.portfolio.enums.StatusProjeto;
import com.codegroup.portfolio.exception.BusinessException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeletionBlockPolicyTest {

    private final DeletionBlockPolicy policy = new DeletionBlockPolicy();

    @ParameterizedTest
    @EnumSource(value = StatusProjeto.class, names = {"INICIADO", "EM_ANDAMENTO", "ENCERRADO"})
    void deveBloquearExclusaoParaStatusProtegidos(StatusProjeto status) {
        assertThatThrownBy(() -> policy.validate(status))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não pode ser excluído");
    }

    @ParameterizedTest
    @EnumSource(value = StatusProjeto.class, names = {"EM_ANALISE", "ANALISE_REALIZADA", "ANALISE_APROVADA", "PLANEJADO", "CANCELADO"})
    void devePermitirExclusaoParaStatusNaoProtegidos(StatusProjeto status) {
        assertThatCode(() -> policy.validate(status)).doesNotThrowAnyException();
    }
}
