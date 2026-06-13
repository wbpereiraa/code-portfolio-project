package com.codegroup.portfolio.repository;

import com.codegroup.portfolio.entity.Projeto;
import com.codegroup.portfolio.enums.StatusProjeto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ProjetoRepository extends JpaRepository<Projeto, Long>, JpaSpecificationExecutor<Projeto> {

    List<Projeto> findByStatus(StatusProjeto status);

    /**
     * Conta quantos projetos com status diferente de ENCERRADO/CANCELADO o membro
     * está alocado (usado na regra de máximo 3 projetos simultâneos por membro).
     */
    @org.springframework.data.jpa.repository.Query("""
            SELECT COUNT(p) FROM Projeto p
            JOIN p.membros m
            WHERE m.id = :membroId
            AND p.status NOT IN (com.codegroup.portfolio.enums.StatusProjeto.ENCERRADO,
                                  com.codegroup.portfolio.enums.StatusProjeto.CANCELADO)
            AND (:projetoIdExcluir IS NULL OR p.id <> :projetoIdExcluir)
            """)
    long countProjetosAtivosPorMembro(Long membroId, Long projetoIdExcluir);

    Page<Projeto> findAll(org.springframework.data.jpa.domain.Specification<Projeto> spec, Pageable pageable);
}
