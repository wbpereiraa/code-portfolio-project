package com.codegroup.portfolio.controller;

import com.codegroup.portfolio.dto.RelatorioPortfolioDTO;
import com.codegroup.portfolio.service.RelatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/relatorios")
@RequiredArgsConstructor
@Tag(name = "Relatórios", description = "Relatórios resumidos do portfólio")
public class RelatorioController {

    private final RelatorioService relatorioService;

    @Operation(summary = "Gera relatório resumido do portfólio: contagem e orçamento por status, "
            + "média de duração dos projetos encerrados e total de membros únicos alocados")
    @GetMapping("/portfolio")
    public ResponseEntity<RelatorioPortfolioDTO> relatorioPortfolio() {
        return ResponseEntity.ok(relatorioService.gerarRelatorio());
    }
}
