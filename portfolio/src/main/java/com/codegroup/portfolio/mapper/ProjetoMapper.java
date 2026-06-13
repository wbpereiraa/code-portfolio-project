package com.codegroup.portfolio.mapper;

import com.codegroup.portfolio.dto.ProjetoResponseDTO;
import com.codegroup.portfolio.entity.Projeto;
import com.codegroup.portfolio.enums.ClassificacaoRisco;

import java.util.List;

public final class ProjetoMapper {

    private ProjetoMapper() {
    }

    public static ProjetoResponseDTO toResponseDTO(Projeto projeto, ClassificacaoRisco classificacaoRisco) {
        List<com.codegroup.portfolio.dto.MembroDTO> membros = projeto.getMembros() == null
                ? List.of()
                : projeto.getMembros().stream()
                    .map(MembroMapper::toDTO)
                    .toList();

        return new ProjetoResponseDTO(
                projeto.getId(),
                projeto.getNome(),
                projeto.getDataInicio(),
                projeto.getPrevisaoTermino(),
                projeto.getDataRealTermino(),
                projeto.getOrcamentoTotal(),
                projeto.getDescricao(),
                MembroMapper.toDTO(projeto.getGerenteResponsavel()),
                projeto.getStatus(),
                classificacaoRisco,
                membros
        );
    }
}
