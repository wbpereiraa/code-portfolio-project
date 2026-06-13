package com.codegroup.portfolio.service;

import com.codegroup.portfolio.client.MembroExternalClient;
import com.codegroup.portfolio.dto.MembroDTO;
import com.codegroup.portfolio.entity.Membro;
import com.codegroup.portfolio.enums.AtribuicaoMembro;
import com.codegroup.portfolio.exception.BusinessException;
import com.codegroup.portfolio.mapper.MembroMapper;
import com.codegroup.portfolio.repository.MembroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Serviço responsável por orquestrar o acesso aos membros.
 * O cadastro "oficial" de membros acontece na API externa mockada
 * (via {@link MembroExternalClient}); este serviço consulta essa API
 * e mantém um cache local (tabela membro) apenas para permitir o
 * relacionamento JPA com Projeto.
 */
@Service
@RequiredArgsConstructor
public class MembroService {

    private final MembroExternalClient membroExternalClient;
    private final MembroRepository membroRepository;

    public List<MembroDTO> listarTodos() {
        return membroExternalClient.listarTodos();
    }

    public MembroDTO buscarPorId(Long id) {
        return membroExternalClient.buscarPorId(id);
    }

    public MembroDTO criar(MembroDTO dto) {
        return membroExternalClient.criar(dto);
    }

    /**
     * Busca o membro na API externa, valida que a atribuição é "FUNCIONARIO"
     * (regra obrigatória para associação a projetos) e garante que ele exista
     * no cache local (tabela membro) para o relacionamento JPA.
     */
    public Membro obterFuncionarioParaAssociacao(Long membroId) {
        MembroDTO membroDTO = membroExternalClient.buscarPorId(membroId);

        if (membroDTO.atribuicao() != AtribuicaoMembro.FUNCIONARIO) {
            throw new BusinessException(
                    "Apenas membros com atribuição FUNCIONARIO podem ser associados a projetos. "
                            + "Membro '" + membroDTO.nome() + "' possui atribuição " + membroDTO.atribuicao()
            );
        }

        return sincronizarCacheLocal(membroDTO);
    }

    /**
     * Busca o membro na API externa e garante que ele exista no cache local,
     * sem restrição de atribuição (usado para o gerente responsável, por exemplo).
     */
    public Membro obterMembroSincronizado(Long membroId) {
        MembroDTO membroDTO = membroExternalClient.buscarPorId(membroId);
        return sincronizarCacheLocal(membroDTO);
    }

    private Membro sincronizarCacheLocal(MembroDTO membroDTO) {
        Membro membro = MembroMapper.toEntity(membroDTO);
        return membroRepository.findById(membro.getId())
                .map(existente -> {
                    existente.setNome(membro.getNome());
                    existente.setAtribuicao(membro.getAtribuicao());
                    return membroRepository.save(existente);
                })
                .orElseGet(() -> membroRepository.save(membro));
    }
}
