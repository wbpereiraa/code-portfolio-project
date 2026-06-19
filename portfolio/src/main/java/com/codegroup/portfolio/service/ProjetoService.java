package com.codegroup.portfolio.service;

import com.codegroup.portfolio.dto.AtualizarStatusDTO;
import com.codegroup.portfolio.dto.ProjetoRequestDTO;
import com.codegroup.portfolio.dto.ProjetoResponseDTO;
import com.codegroup.portfolio.entity.Membro;
import com.codegroup.portfolio.entity.Projeto;
import com.codegroup.portfolio.enums.StatusProjeto;
import com.codegroup.portfolio.exception.ResourceNotFoundException;
import com.codegroup.portfolio.mapper.ProjetoMapper;
import com.codegroup.portfolio.policy.DeletionBlockPolicy;
import com.codegroup.portfolio.policy.MemberAllocationValidator;
import com.codegroup.portfolio.policy.StatusTransitionPolicy;
import com.codegroup.portfolio.repository.ProjetoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjetoService {

    private final ProjetoRepository projetoRepository;
    private final MembroService membroService;
    private final ClassificacaoRiscoService classificacaoRiscoService;
    private final StatusTransitionPolicy statusTransitionPolicy;
    private final DeletionBlockPolicy deletionBlockPolicy;
    private final MemberAllocationValidator memberAllocationValidator;

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
        memberAllocationValidator.validateDates(dto.dataInicio(), dto.previsaoTermino(), dto.dataRealTermino());

        Projeto projeto = new Projeto();
        aplicarDadosBasicos(projeto, dto);

        projeto.setStatus(StatusProjeto.EM_ANALISE);

        Membro gerente = membroService.obterMembroSincronizado(dto.gerenteResponsavelId());
        projeto.setGerenteResponsavel(gerente);

        List<Membro> membros = memberAllocationValidator.validateAndResolve(dto.membrosIds(), null);
        projeto.setMembros(membros);

        Projeto salvo = projetoRepository.save(projeto);
        return toResponseDTO(salvo);
    }

    @Transactional
    public ProjetoResponseDTO atualizar(Long id, ProjetoRequestDTO dto) {
        Projeto projeto = buscarEntidadePorId(id);

        memberAllocationValidator.validateDates(dto.dataInicio(), dto.previsaoTermino(), dto.dataRealTermino());

        aplicarDadosBasicos(projeto, dto);

        Membro gerente = membroService.obterMembroSincronizado(dto.gerenteResponsavelId());
        projeto.setGerenteResponsavel(gerente);

        List<Membro> membros = memberAllocationValidator.validateAndResolve(dto.membrosIds(), projeto.getId());
        projeto.setMembros(membros);

        if (dto.status() != null && dto.status() != projeto.getStatus()) {
            statusTransitionPolicy.validate(projeto.getStatus(), dto.status());
            projeto.setStatus(dto.status());
        }

        Projeto salvo = projetoRepository.save(projeto);
        return toResponseDTO(salvo);
    }

    @Transactional
    public ProjetoResponseDTO atualizarStatus(Long id, AtualizarStatusDTO dto) {
        Projeto projeto = buscarEntidadePorId(id);

        statusTransitionPolicy.validate(projeto.getStatus(), dto.novoStatus());
        projeto.setStatus(dto.novoStatus());

        Projeto salvo = projetoRepository.save(projeto);
        return toResponseDTO(salvo);
    }

    @Transactional
    public void deletar(Long id) {
        Projeto projeto = buscarEntidadePorId(id);
        deletionBlockPolicy.validate(projeto.getStatus());
        projetoRepository.delete(projeto);
    }

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

    private ProjetoResponseDTO toResponseDTO(Projeto projeto) {
        var classificacao = classificacaoRiscoService.calcular(
                projeto.getOrcamentoTotal(),
                projeto.getDataInicio(),
                projeto.getPrevisaoTermino()
        );
        return ProjetoMapper.toResponseDTO(projeto, classificacao);
    }
}
