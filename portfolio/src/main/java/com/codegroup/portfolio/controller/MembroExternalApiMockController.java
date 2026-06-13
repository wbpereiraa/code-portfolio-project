package com.codegroup.portfolio.controller;

import com.codegroup.portfolio.dto.MembroDTO;
import com.codegroup.portfolio.enums.AtribuicaoMembro;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MOCK de uma API REST EXTERNA de cadastro de membros.
 *
 * Em um cenário real, esse cadastro viveria em outro sistema/serviço.
 * Para fins deste desafio, expomos esse endpoint dentro da própria aplicação
 * (prefixo /external) para simular a integração: o {@code MembroExternalClient}
 * consome esses endpoints como se fossem de um serviço externo.
 */
@RestController
@RequestMapping("/external/membros")
public class MembroExternalApiMockController {

    private final Map<Long, MembroDTO> membros = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    public MembroExternalApiMockController() {
        // dados iniciais de exemplo
        criar(new MembroDTO(null, "Ana Souza", AtribuicaoMembro.GERENTE));
        criar(new MembroDTO(null, "Bruno Lima", AtribuicaoMembro.FUNCIONARIO));
        criar(new MembroDTO(null, "Carla Mendes", AtribuicaoMembro.FUNCIONARIO));
        criar(new MembroDTO(null, "Diego Alves", AtribuicaoMembro.FUNCIONARIO));
        criar(new MembroDTO(null, "Elaine Costa", AtribuicaoMembro.CONSULTOR));
    }

    @GetMapping
    public ResponseEntity<List<MembroDTO>> listar() {
        return ResponseEntity.ok(List.copyOf(membros.values()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MembroDTO> buscarPorId(@PathVariable Long id) {
        MembroDTO membro = membros.get(id);
        if (membro == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(membro);
    }

    @PostMapping
    public ResponseEntity<MembroDTO> criar(@Valid @RequestBody MembroDTO dto) {
        long id = sequence.incrementAndGet();
        MembroDTO novo = new MembroDTO(id, dto.nome(), dto.atribuicao());
        membros.put(id, novo);
        return ResponseEntity.status(HttpStatus.CREATED).body(novo);
    }
}
