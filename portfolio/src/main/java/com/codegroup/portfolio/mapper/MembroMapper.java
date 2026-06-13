package com.codegroup.portfolio.mapper;

import com.codegroup.portfolio.dto.MembroDTO;
import com.codegroup.portfolio.entity.Membro;

public final class MembroMapper {

    private MembroMapper() {
    }

    public static Membro toEntity(MembroDTO dto) {
        if (dto == null) {
            return null;
        }
        return Membro.builder()
                .id(dto.id())
                .nome(dto.nome())
                .atribuicao(dto.atribuicao())
                .build();
    }

    public static MembroDTO toDTO(Membro entity) {
        if (entity == null) {
            return null;
        }
        return new MembroDTO(entity.getId(), entity.getNome(), entity.getAtribuicao());
    }
}
