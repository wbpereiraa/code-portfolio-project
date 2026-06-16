package com.codegroup.portfolio.service;

import com.codegroup.portfolio.entity.RefreshToken;
import com.codegroup.portfolio.entity.Usuario;
import com.codegroup.portfolio.repository.RefreshTokenRepository;
import com.codegroup.portfolio.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Transactional
    public RefreshToken criar(Usuario usuario) {
        // revoga todos os refresh tokens anteriores do usuário (rotação de token)
        refreshTokenRepository.revogarTodosPorUsuario(usuario);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .usuario(usuario)
                .expiresAt(Instant.now().plusMillis(refreshTokenExpirationMs))
                .revogado(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public RefreshToken validarEBuscar(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("Refresh token inválido ou não encontrado."));

        if (refreshToken.isRevogado()) {
            throw new BusinessException("Refresh token já foi revogado.");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Refresh token expirado. Faça login novamente.");
        }

        return refreshToken;
    }

    @Transactional
    public void revogarPorUsuario(Usuario usuario) {
        refreshTokenRepository.revogarTodosPorUsuario(usuario);
    }

    /**
     * Limpeza automática de tokens expirados e revogados: roda diariamente à meia-noite.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void limparTokensExpirados() {
        log.info("Iniciando limpeza de refresh tokens expirados/revogados...");
        refreshTokenRepository.deletarExpiradosERevogados(Instant.now());
        log.info("Limpeza concluída.");
    }
}
