package com.codegroup.portfolio.client;

import com.codegroup.portfolio.dto.MembroDTO;
import com.codegroup.portfolio.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Cliente responsável por consumir a API REST externa (mockada) de membros.
 * Toda comunicação com o "serviço de membros" passa por esta classe,
 * mantendo a integração isolada do restante da aplicação.
 */
@Component
@Slf4j
public class MembroExternalClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public MembroExternalClient(RestTemplate restTemplate,
                                 @Value("${membros.api.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public List<MembroDTO> listarTodos() {
        MembroDTO[] response = restTemplate.getForObject(baseUrl + "/external/membros", MembroDTO[].class);
        return response != null ? List.of(response) : List.of();
    }

    public MembroDTO buscarPorId(Long id) {
        try {
            MembroDTO membro = restTemplate.getForObject(baseUrl + "/external/membros/" + id, MembroDTO.class);
            if (membro == null) {
                throw new ResourceNotFoundException("Membro não encontrado na API externa com id: " + id);
            }
            return membro;
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Membro não encontrado na API externa com id: " + id);
        }
    }

    public MembroDTO criar(MembroDTO dto) {
        HttpEntity<MembroDTO> request = new HttpEntity<>(dto);
        return restTemplate.exchange(
                baseUrl + "/external/membros",
                HttpMethod.POST,
                request,
                MembroDTO.class
        ).getBody();
    }
}
