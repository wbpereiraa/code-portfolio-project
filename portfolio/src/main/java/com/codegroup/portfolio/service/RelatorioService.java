package com.codegroup.portfolio.service;

import com.codegroup.portfolio.dto.RelatorioPortfolioDTO;
import com.codegroup.portfolio.entity.Membro;
import com.codegroup.portfolio.entity.Projeto;
import com.codegroup.portfolio.enums.StatusProjeto;
import com.codegroup.portfolio.repository.ProjetoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RelatorioService {

    private final ProjetoRepository projetoRepository;

    @Transactional(readOnly = true)
    public RelatorioPortfolioDTO gerarRelatorio() {
        List<Projeto> todosProjetos = projetoRepository.findAll();

        Map<StatusProjeto, Long> quantidadePorStatus = new EnumMap<>(StatusProjeto.class);
        Map<StatusProjeto, BigDecimal> orcamentoPorStatus = new EnumMap<>(StatusProjeto.class);

        for (StatusProjeto status : StatusProjeto.values()) {
            quantidadePorStatus.put(status, 0L);
            orcamentoPorStatus.put(status, BigDecimal.ZERO);
        }

        for (Projeto projeto : todosProjetos) {
            StatusProjeto status = projeto.getStatus();
            quantidadePorStatus.merge(status, 1L, Long::sum);
            orcamentoPorStatus.merge(status, projeto.getOrcamentoTotal(), BigDecimal::add);
        }

        Double mediaDuracaoEncerrados = calcularMediaDuracaoEncerrados(todosProjetos);

        long totalMembrosUnicos = todosProjetos.stream()
                .flatMap(p -> p.getMembros().stream())
                .map(Membro::getId)
                .collect(Collectors.toSet())
                .size();

        return new RelatorioPortfolioDTO(
                quantidadePorStatus,
                orcamentoPorStatus,
                mediaDuracaoEncerrados,
                totalMembrosUnicos
        );
    }

    private Double calcularMediaDuracaoEncerrados(List<Projeto> projetos) {
        List<Projeto> encerrados = projetos.stream()
                .filter(p -> p.getStatus() == StatusProjeto.ENCERRADO)
                .filter(p -> p.getDataRealTermino() != null)
                .toList();

        if (encerrados.isEmpty()) {
            return 0.0;
        }

        double mediaEmDias = encerrados.stream()
                .mapToLong(p -> ChronoUnit.DAYS.between(p.getDataInicio(), p.getDataRealTermino()))
                .average()
                .orElse(0.0);

        return mediaEmDias;
    }
}
