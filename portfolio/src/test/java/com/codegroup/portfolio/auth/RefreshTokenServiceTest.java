package com.codegroup.portfolio.auth;

import com.codegroup.portfolio.entity.RefreshToken;
import com.codegroup.portfolio.entity.Usuario;
import com.codegroup.portfolio.repository.RefreshTokenRepository;
import com.codegroup.portfolio.service.RefreshTokenService;
import com.codegroup.portfolio.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpirationMs", 604800000L);
        usuario = Usuario.builder()
                .id(1L)
                .username("admin")
                .password("encoded")
                .roles(Set.of("ADMIN"))
                .ativo(true)
                .build();
    }

    @Test
    void deveCriarRefreshTokenComSucesso() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken token = refreshTokenService.criar(usuario);

        assertThat(token.getToken()).isNotBlank();
        assertThat(token.getUsuario()).isEqualTo(usuario);
        assertThat(token.isRevogado()).isFalse();
        assertThat(token.getExpiresAt()).isAfter(Instant.now());
        verify(refreshTokenRepository).revogarTodosPorUsuario(usuario);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void deveLancarExcecaoParaTokenInexistente() {
        when(refreshTokenRepository.findByToken("invalido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.validarEBuscar("invalido"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inválido ou não encontrado");
    }

    @Test
    void deveLancarExcecaoParaTokenRevogado() {
        RefreshToken tokenRevogado = RefreshToken.builder()
                .token("abc")
                .usuario(usuario)
                .expiresAt(Instant.now().plusSeconds(3600))
                .revogado(true)
                .build();

        when(refreshTokenRepository.findByToken("abc")).thenReturn(Optional.of(tokenRevogado));

        assertThatThrownBy(() -> refreshTokenService.validarEBuscar("abc"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("revogado");
    }

    @Test
    void deveLancarExcecaoParaTokenExpirado() {
        RefreshToken tokenExpirado = RefreshToken.builder()
                .token("abc")
                .usuario(usuario)
                .expiresAt(Instant.now().minusSeconds(1))
                .revogado(false)
                .build();

        when(refreshTokenRepository.findByToken("abc")).thenReturn(Optional.of(tokenExpirado));

        assertThatThrownBy(() -> refreshTokenService.validarEBuscar("abc"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("expirado");
    }

    @Test
    void deveRetornarTokenValidoComSucesso() {
        RefreshToken tokenValido = RefreshToken.builder()
                .token("abc")
                .usuario(usuario)
                .expiresAt(Instant.now().plusSeconds(3600))
                .revogado(false)
                .build();

        when(refreshTokenRepository.findByToken("abc")).thenReturn(Optional.of(tokenValido));

        RefreshToken resultado = refreshTokenService.validarEBuscar("abc");

        assertThat(resultado.getToken()).isEqualTo("abc");
    }
}
