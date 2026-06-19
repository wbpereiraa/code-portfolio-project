package com.codegroup.portfolio.policy;

import com.codegroup.portfolio.entity.Membro;
import com.codegroup.portfolio.exception.BusinessException;
import com.codegroup.portfolio.repository.ProjetoRepository;
import com.codegroup.portfolio.service.MembroService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MemberAllocationValidator {

    private static final int MIN_MEMBROS = 1;
    private static final int MAX_MEMBROS = 10;
    private static final int MAX_PROJETOS_ATIVOS_POR_MEMBRO = 3;

    private final MembroService membroService;
    private final ProjetoRepository projetoRepository;

    public List<Membro> validateAndResolve(List<Long> membrosIds, Long projetoIdAtual) {
        if (membrosIds == null || membrosIds.size() < MIN_MEMBROS || membrosIds.size() > MAX_MEMBROS) {
            throw new BusinessException(
                    "O projeto deve ter entre " + MIN_MEMBROS + " e " + MAX_MEMBROS + " membros."
            );
        }

        if (membrosIds.stream().distinct().count() != membrosIds.size()) {
            throw new BusinessException("A lista de membros não pode conter ids duplicados.");
        }

        List<Membro> membros = new ArrayList<>();

        for (Long membroId : membrosIds) {
            Membro membro = membroService.obterFuncionarioParaAssociacao(membroId);

            long projetosAtivos = projetoRepository.countProjetosAtivosPorMembro(membro.getId(), projetoIdAtual);
            if (projetosAtivos >= MAX_PROJETOS_ATIVOS_POR_MEMBRO) {
                throw new BusinessException(
                        "Membro '" + membro.getNome() + "' já está alocado em "
                                + MAX_PROJETOS_ATIVOS_POR_MEMBRO
                                + " projetos ativos (status diferente de ENCERRADO/CANCELADO)."
                );
            }

            membros.add(membro);
        }

        return membros;
    }

    public void validateDates(java.time.LocalDate dataInicio,
                              java.time.LocalDate previsaoTermino,
                              java.time.LocalDate dataRealTermino) {
        if (previsaoTermino.isBefore(dataInicio)) {
            throw new BusinessException("A previsão de término não pode ser anterior à data de início.");
        }
        if (dataRealTermino != null && dataRealTermino.isBefore(dataInicio)) {
            throw new BusinessException("A data real de término não pode ser anterior à data de início.");
        }
    }
}
