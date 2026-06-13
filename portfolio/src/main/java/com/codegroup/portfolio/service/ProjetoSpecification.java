package com.codegroup.portfolio.service;

import com.codegroup.portfolio.entity.Projeto;
import com.codegroup.portfolio.enums.StatusProjeto;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * Specifications (filtros dinâmicos) para listagem paginada de projetos.
 */
public final class ProjetoSpecification {

    private ProjetoSpecification() {
    }

    public static Specification<Projeto> comFiltros(String nome, StatusProjeto status,
                                                      LocalDate dataInicioDe, LocalDate dataInicioAte) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();

            if (nome != null && !nome.isBlank()) {
                predicates = cb.and(predicates,
                        cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%"));
            }

            if (status != null) {
                predicates = cb.and(predicates, cb.equal(root.get("status"), status));
            }

            if (dataInicioDe != null) {
                predicates = cb.and(predicates, cb.greaterThanOrEqualTo(root.get("dataInicio"), dataInicioDe));
            }

            if (dataInicioAte != null) {
                predicates = cb.and(predicates, cb.lessThanOrEqualTo(root.get("dataInicio"), dataInicioAte));
            }

            return predicates;
        };
    }
}
