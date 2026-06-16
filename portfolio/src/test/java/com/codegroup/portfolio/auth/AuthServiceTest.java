package com.codegroup.portfolio.service;

import com.codegroup.portfolio.dto.*;
import com.codegroup.portfolio.entity.RefreshToken;
import com.codegroup.portfolio.entity.Usuario;
import com.codegroup.portfolio.repository.UsuarioRepository;
import com.codegroup.portfolio.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private Usuario usuario;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        Set<String> roles = new HashSet<>();
        roles.add("ADMIN");
        roles.add("USER");

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("admin");
        usuario.setPassword("encoded_password");
        usuario.setRoles(roles);
        usuario.setAtivo(true);

        refreshToken = RefreshToken.builder()
                .id(1L)
                .token("refresh-uuid-123")
                .usuario(usuario)
                .expiresAt(Instant.now().plusSeconds(3600))
                .revogado(false)
                .build();
    }

    // -----------------------------------------------------------------------
    // login
    // -----------------------------------------------------------------------

    @Test
    void deveRealizarLoginComSucesso() {
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        when(jwtService.gerarAccessToken(any())).thenReturn("access-token-xyz");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900000L);
        when(refreshTokenService.criar(usuario)).thenReturn(refreshToken);

        TokenResponseDTO response = authService.login(new LoginRequestDTO("admin", "admin123"));

        assertThat(response.accessToken()).isEqualTo("access-token-xyz");
        assertThat(response.refreshToken()).isEqualTo("refresh-uuid-123");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900000L);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void deveLancarExcecaoQuandoCredenciaisInvalidas() {
        doThrow(new BadCredentialsException("bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(new LoginRequestDTO("admin", "errada")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Credenciais inválidas");

        verify(usuarioRepository, never()).findByUsername(any());
    }

    // -----------------------------------------------------------------------
    // refresh
    // -----------------------------------------------------------------------

    @Test
    void deveRenovarTokenComSucesso() {
        RefreshToken novoRefresh = RefreshToken.builder()
                .token("novo-refresh-uuid")
                .usuario(usuario)
                .expiresAt(Instant.now().plusSeconds(7200))
                .revogado(false)
                .build();

        when(refreshTokenService.validarEBuscar("refresh-uuid-123")).thenReturn(refreshToken);
        when(jwtService.gerarAccessToken(any())).thenReturn("novo-access-token");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900000L);
        when(refreshTokenService.criar(usuario)).thenReturn(novoRefresh);

        TokenResponseDTO response = authService.refresh(new RefreshRequestDTO("refresh-uuid-123"));

        assertThat(response.accessToken()).isEqualTo("novo-access-token");
        assertThat(response.refreshToken()).isEqualTo("novo-refresh-uuid");
        verify(refreshTokenService).criar(usuario); // rotação: novo refresh token emitido
    }

    // -----------------------------------------------------------------------
    // logout
    // -----------------------------------------------------------------------

    @Test
    void deveRealizarLogoutRevogandoTokens() {
        when(refreshTokenService.validarEBuscar("refresh-uuid-123")).thenReturn(refreshToken);

        authService.logout(new RefreshRequestDTO("refresh-uuid-123"));

        verify(refreshTokenService).revogarPorUsuario(usuario);
    }

    @Test
    void deveLancarExcecaoNoLogoutComTokenInvalido() {
        when(refreshTokenService.validarEBuscar("invalido"))
                .thenThrow(new BusinessException("Refresh token inválido ou não encontrado."));

        assertThatThrownBy(() -> authService.logout(new RefreshRequestDTO("invalido")))
                .isInstanceOf(BusinessException.class);

        verify(refreshTokenService, never()).revogarPorUsuario(any());
    }

    // -----------------------------------------------------------------------
    // registrar
    // -----------------------------------------------------------------------

    @Test
    void deveRegistrarNovoUsuarioComSucesso() {
        when(usuarioRepository.existsByUsername("novo")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("encoded");

        authService.registrar(new RegisterRequestDTO("novo", "senha123", Set.of("USER")));

        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void deveAtribuirRoleUserPadraoQuandoRolesNaoInformadas() {
        when(usuarioRepository.existsByUsername("novo")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");

        authService.registrar(new RegisterRequestDTO("novo", "senha123", null));

        verify(usuarioRepository).save(argThat(u -> u.getRoles().contains("USER")));
    }

    @Test
    void deveLancarExcecaoQuandoUsernameJaExiste() {
        when(usuarioRepository.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(() -> authService.registrar(new RegisterRequestDTO("admin", "123456", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("já está em uso");

        verify(usuarioRepository, never()).save(any());
    }
}
