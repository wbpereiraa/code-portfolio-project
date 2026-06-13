package com.codegroup.portfolio.service;

import com.codegroup.portfolio.dto.AtualizarStatusDTO;
import com.codegroup.portfolio.dto.ProjetoRequestDTO;
import com.codegroup.portfolio.dto.ProjetoResponseDTO;
import com.codegroup.portfolio.entity.Membro;
import com.codegroup.portfolio.entity.Projeto;
import com.codegroup.portfolio.enums.StatusProjeto;
import com.codegroup.portfolio.exception.BusinessException;
import com.codegroup.portfolio.exception.ResourceNotFoundException;
import com.codegroup.portfolio.mapper.ProjetoMapper;
import com.codegroup.portfolio.repository.ProjetoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjetoService {

    private static final int MIN_MEMBROS = 1;
    private static final int MAX_MEMBROS = 10;
    private static final int MAX_PROJETOS_ATIVOS_POR_MEMBRO = 3;

    private static final Set<StatusProjeto> STATUS_IMPEDEM_EXCLUSAO = Set.of(
            StatusProjeto.INICIADO, StatusProjeto.EM_ANDAMENTO, StatusProjeto.ENCERRADO
    );

    private final ProjetoRepository projetoRepository;
    private final MembroService membroService;
    private final ClassificacaoRiscoService classificacaoRiscoService;

    @Transactional(readOnly = true)
    public Page<ProjetoResponseDTO> listar(String nome, StatusProjeto status,
                                            LocalDate dataInicioDe, LocalDate dataInicioAte,
                                            Pageable pageable) {
        var spec = ProjetoSpecification.comFiltros(nome, status, dataInicioDe, dataInicioAte);
        return projetoRepository.findAll(spec, pageable)
                .map(this::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public ProjetoResponseDTO buscarPorId(Long id) {
        Projeto projeto = buscarEntidadePorId(id);
        return toResponseDTO(projeto);
    }

    @Transactional
    public ProjetoResponseDTO criar(ProjetoRequestDTO dto) {
        validarDatas(dto.dataInicio(), dto.previsaoTermino(), dto.dataRealTermino());

        Projeto projeto = new Projeto();
        aplicarDadosBasicos(projeto, dto);

        projeto.setStatus(StatusProjeto.EM_ANALISE);

        Membro gerente = membroService.obterMembroSincronizado(dto.gerenteResponsavelId());
        projeto.setGerenteResponsavel(gerente);

        List<Membro> membros = resolverEValidarMembros(dto.membrosIds(), null);
        projeto.setMembros(membros);

        Projeto salvo = projetoRepository.save(projeto);
        return toResponseDTO(salvo);
    }

    @Transactional
    public ProjetoResponseDTO atualizar(Long id, ProjetoRequestDTO dto) {
        Projeto projeto = buscarEntidadePorId(id);

        validarDatas(dto.dataInicio(), dto.previsaoTermino(), dto.dataRealTermino());

        aplicarDadosBasicos(projeto, dto);

        Membro gerente = membroService.obterMembroSincronizado(dto.gerenteResponsavelId());
        projeto.setGerenteResponsavel(gerente);

        List<Membro> membros = resolverEValidarMembros(dto.membrosIds(), projeto.getId());
        projeto.setMembros(membros);

        // se o DTO trouxer um status diferente do atual, valida a transição
        if (dto.status() != null && dto.status() != projeto.getStatus()) {
            validarTransicaoStatus(projeto.getStatus(), dto.status());
            projeto.setStatus(dto.status());
        }

        Projeto salvo = projetoRepository.save(projeto);
        return toResponseDTO(salvo);
    }

    @Transactional
    public ProjetoResponseDTO atualizarStatus(Long id, AtualizarStatusDTO dto) {
        Projeto projeto = buscarEntidadePorId(id);

        validarTransicaoStatus(projeto.getStatus(), dto.novoStatus());
        projeto.setStatus(dto.novoStatus());

        Projeto salvo = projetoRepository.save(projeto);
        return toResponseDTO(salvo);
    }

    @Transactional
    public void deletar(Long id) {
        Projeto projeto = buscarEntidadePorId(id);

        if (STATUS_IMPEDEM_EXCLUSAO.contains(projeto.getStatus())) {
            throw new BusinessException(
                    "Projeto com status " + projeto.getStatus() + " não pode ser excluído."
            );
        }

        projetoRepository.delete(projeto);
    }

    // ---------------------------------------------------------------------
    // Métodos auxiliares
    // ---------------------------------------------------------------------

    private Projeto buscarEntidadePorId(Long id) {
        return projetoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado com id: " + id));
    }

    private void aplicarDadosBasicos(Projeto projeto, ProjetoRequestDTO dto) {
        projeto.setNome(dto.nome());
        projeto.setDataInicio(dto.dataInicio());
        projeto.setPrevisaoTermino(dto.previsaoTermino());
        projeto.setDataRealTermino(dto.dataRealTermino());
        projeto.setOrcamentoTotal(dto.orcamentoTotal());
        projeto.setDescricao(dto.descricao());
    }

    private void validarDatas(LocalDate dataInicio, LocalDate previsaoTermino, LocalDate dataRealTermino) {
        if (previsaoTermino.isBefore(dataInicio)) {
            throw new BusinessException("A previsão de término não pode ser anterior à data de início.");
        }
        if (dataRealTermino != null && dataRealTermino.isBefore(dataInicio)) {
            throw new BusinessException("A data real de término não pode ser anterior à data de início.");
        }
    }

    /**
     * Valida a transição de status conforme a sequência lógica definida em
     * {@link StatusProjeto#podeTransicionarPara(StatusProjeto)}.
     */
    private void validarTransicaoStatus(StatusProjeto statusAtual, StatusProjeto novoStatus) {
        if (statusAtual == novoStatus) {
            return;
        }

        if (!statusAtual.podeTransicionarPara(novoStatus)) {
            throw new BusinessException(
                    "Transição de status inválida: não é possível ir de " + statusAtual + " para " + novoStatus + "."
            );
        }
    }

    /**
     * Resolve a lista de membros a partir dos ids informados, validando:
     *  - quantidade entre MIN_MEMBROS e MAX_MEMBROS;
     *  - cada membro tem atribuição FUNCIONARIO;
     *  - cada membro não está alocado em mais de MAX_PROJETOS_ATIVOS_POR_MEMBRO
     *    projetos com status diferente de ENCERRADO/CANCELADO (desconsiderando o próprio
     *    projeto em caso de atualização).
     */
    private List<Membro> resolverEValidarMembros(List<Long> membrosIds, Long projetoIdAtual) {
        if (membrosIds == null || membrosIds.size() < MIN_MEMBROS || membrosIds.size() > MAX_MEMBROS) {
            throw new BusinessException(
                    "O projeto deve ter entre " + MIN_MEMBROS + " e " + MAX_MEMBROS + " membros."
            );
        }

        if (membrosIds.stream().distinct().count() != membrosIds.size()) {
            throw new BusinessException("A lista de membros não pode conter ids duplicados.");
        }

        List<Membro> membros = new ArrayList<>();

        for (Long membroId : membrosIds) {
            Membro membro = membroService.obterFuncionarioParaAssociacao(membroId);

            long projetosAtivos = projetoRepository.countProjetosAtivosPorMembro(membro.getId(), projetoIdAtual);
            if (projetosAtivos >= MAX_PROJETOS_ATIVOS_POR_MEMBRO) {
                throw new BusinessException(
                        "Membro '" + membro.getNome() + "' já está alocado em "
                                + MAX_PROJETOS_ATIVOS_POR_MEMBRO
                                + " projetos ativos (status diferente de ENCERRADO/CANCELADO)."
                );
            }

            membros.add(membro);
        }

        return membros;
    }

    private ProjetoResponseDTO toResponseDTO(Projeto projeto) {
        var classificacao = classificacaoRiscoService.calcular(
                projeto.getOrcamentoTotal(),
                projeto.getDataInicio(),
                projeto.getPrevisaoTermino()
        );
        return ProjetoMapper.toResponseDTO(projeto, classificacao);
    }
}
