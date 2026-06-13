package com.codegroup.portfolio.controller;

import com.codegroup.portfolio.dto.AtualizarStatusDTO;
import com.codegroup.portfolio.dto.ProjetoRequestDTO;
import com.codegroup.portfolio.dto.ProjetoResponseDTO;
import com.codegroup.portfolio.enums.StatusProjeto;
import com.codegroup.portfolio.service.ProjetoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/projetos")
@RequiredArgsConstructor
@Tag(name = "Projetos", description = "Gerenciamento de projetos do portfólio")
public class ProjetoController {

    private final ProjetoService projetoService;

    @Operation(summary = "Lista projetos com paginação e filtros opcionais (nome, status, período de início)")
    @GetMapping
    public ResponseEntity<Page<ProjetoResponseDTO>> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) StatusProjeto status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicioDe,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicioAte,
            @Parameter(hidden = true) Pageable pageable) {
        return ResponseEntity.ok(projetoService.listar(nome, status, dataInicioDe, dataInicioAte, pageable));
    }

    @Operation(summary = "Busca um projeto pelo id, incluindo classificação de risco calculada")
    @GetMapping("/{id}")
    public ResponseEntity<ProjetoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(projetoService.buscarPorId(id));
    }

    @Operation(summary = "Cria um novo projeto (status inicial sempre EM_ANALISE)")
    @PostMapping
    public ResponseEntity<ProjetoResponseDTO> criar(@Valid @RequestBody ProjetoRequestDTO dto) {
        ProjetoResponseDTO criado = projetoService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @Operation(summary = "Atualiza os dados de um projeto existente")
    @PutMapping("/{id}")
    public ResponseEntity<ProjetoResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody ProjetoRequestDTO dto) {
        return ResponseEntity.ok(projetoService.atualizar(id, dto));
    }

    @Operation(summary = "Atualiza apenas o status do projeto, respeitando a sequência lógica permitida")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ProjetoResponseDTO> atualizarStatus(@PathVariable Long id, @Valid @RequestBody AtualizarStatusDTO dto) {
        return ResponseEntity.ok(projetoService.atualizarStatus(id, dto));
    }

    @Operation(summary = "Exclui um projeto (não permitido se status for INICIADO, EM_ANDAMENTO ou ENCERRADO)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        projetoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
