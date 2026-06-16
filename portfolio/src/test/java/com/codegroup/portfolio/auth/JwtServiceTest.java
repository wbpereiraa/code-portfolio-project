package com.codegroup.portfolio.auth;

import com.codegroup.portfolio.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "3f8a2c9b1d7e4f6a0c5b8d2e1f4a7c9b3d6e8f2a5c1b4d7e0f3a6c9b2d5e8f1";
    private static final long EXPIRATION_MS = 900000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION_MS);
    }

    private UserDetails buildUser(String username) {
        return User.builder()
                .username(username)
                .password("senha")
                .authorities(List.of())
                .build();
    }

    @Test
    void deveGerarTokenEExtrairUsernameCorretamente() {
        UserDetails user = buildUser("admin");
        String token = jwtService.gerarAccessToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extrairUsername(token)).isEqualTo("admin");
    }

    @Test
    void deveValidarTokenGeradoParaOMesmoUsuario() {
        UserDetails user = buildUser("admin");
        String token = jwtService.gerarAccessToken(user);

        assertThat(jwtService.isTokenValido(token, user)).isTrue();
    }

    @Test
    void naoDeveValidarTokenParaUsuarioDiferente() {
        UserDetails admin = buildUser("admin");
        UserDetails outro = buildUser("outro");

        String token = jwtService.gerarAccessToken(admin);

        assertThat(jwtService.isTokenValido(token, outro)).isFalse();
    }

    @Test
    void deveDetectarTokenExpirado() {
        // Token gerado com expiração no passado deve lançar ExpiredJwtException ao ser parseado
        JwtService serviceComExpiracaoImediata = new JwtService(SECRET, -1000L);
        UserDetails user = buildUser("admin");
        String token = serviceComExpiracaoImediata.gerarAccessToken(user);

        // isTokenValido captura JwtException e retorna false — token expirado = inválido
        assertThat(serviceComExpiracaoImediata.isTokenValido(token, user)).isFalse();
    }

    @Test
    void deveRetornarTokenNaoExpiradoParaTokenValido() {
        UserDetails user = buildUser("admin");
        String token = jwtService.gerarAccessToken(user);

        assertThat(jwtService.isTokenExpirado(token)).isFalse();
    }

    @Test
    void deveGerarTokensDiferentesParaUsuariosDiferentes() {
        // Tokens para usuários distintos devem ser diferentes
        UserDetails admin = buildUser("admin");
        UserDetails outro = buildUser("outro_usuario");

        String token1 = jwtService.gerarAccessToken(admin);
        String token2 = jwtService.gerarAccessToken(outro);

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void deveGerarTokensDiferentesComExtraClaimsDiferentes() {
        // Tokens com claims extras distintos devem ser diferentes
        UserDetails user = buildUser("admin");

        String token1 = jwtService.gerarAccessToken(java.util.Map.of("role", "ADMIN"), user);
        String token2 = jwtService.gerarAccessToken(java.util.Map.of("role", "USER"), user);

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void deveLancarExcecaoAoExtrairUsernameDeTokenExpirado() {
        JwtService serviceExpirado = new JwtService(SECRET, -1000L);
        UserDetails user = buildUser("admin");
        String token = serviceExpirado.gerarAccessToken(user);

        assertThatThrownBy(() -> serviceExpirado.extrairUsername(token))
                .isInstanceOf(ExpiredJwtException.class);
    }
}