package com.codegroup.portfolio.controller;

import com.codegroup.portfolio.dto.MembroDTO;
import com.codegroup.portfolio.service.MembroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints internos que delegam para a API externa (mockada) de membros.
 */
@RestController
@RequestMapping("/api/membros")
@RequiredArgsConstructor
@Tag(name = "Membros", description = "Consulta/criação de membros via API externa mockada")
public class MembroController {

    private final MembroService membroService;

    @Operation(summary = "Lista todos os membros cadastrados na API externa")
    @GetMapping
    public ResponseEntity<List<MembroDTO>> listarTodos() {
        return ResponseEntity.ok(membroService.listarTodos());
    }

    @Operation(summary = "Busca um membro pelo id na API externa")
    @GetMapping("/{id}")
    public ResponseEntity<MembroDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(membroService.buscarPorId(id));
    }

    @Operation(summary = "Cria um novo membro na API externa (nome + atribuição/cargo)")
    @PostMapping
    public ResponseEntity<MembroDTO> criar(@Valid @RequestBody MembroDTO dto) {
        MembroDTO criado = membroService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }
}
