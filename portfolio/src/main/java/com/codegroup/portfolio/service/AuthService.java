package com.codegroup.portfolio.service;

import com.codegroup.portfolio.dto.*;
import com.codegroup.portfolio.entity.RefreshToken;
import com.codegroup.portfolio.entity.Usuario;
import com.codegroup.portfolio.repository.UsuarioRepository;
import com.codegroup.portfolio.exception.BusinessException;
import com.codegroup.portfolio.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public TokenResponseDTO login(LoginRequestDTO dto) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.username(), dto.password())
            );
        } catch (AuthenticationException e) {
            throw new BusinessException("Credenciais inválidas.");
        }

        Usuario usuario = usuarioRepository.findByUsername(dto.username())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(
                        usuario.getUsername(),
                        usuario.getPassword(),
                        usuario.getRoles().stream()
                                .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role))
                                .toList()
                );

        String accessToken = jwtService.gerarAccessToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.criar(usuario);

        return new TokenResponseDTO(
                accessToken,
                refreshToken.getToken(),
                jwtService.getAccessTokenExpirationMs()
        );
    }

    @Transactional
    public TokenResponseDTO refresh(RefreshRequestDTO dto) {
        RefreshToken refreshToken = refreshTokenService.validarEBuscar(dto.refreshToken());
        Usuario usuario = refreshToken.getUsuario();

        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(
                        usuario.getUsername(),
                        usuario.getPassword(),
                        usuario.getRoles().stream()
                                .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role))
                                .toList()
                );

        // rotação: revoga o refresh token usado e emite um novo
        String novoAccessToken = jwtService.gerarAccessToken(userDetails);
        RefreshToken novoRefreshToken = refreshTokenService.criar(usuario);

        return new TokenResponseDTO(
                novoAccessToken,
                novoRefreshToken.getToken(),
                jwtService.getAccessTokenExpirationMs()
        );
    }

    @Transactional
    public void logout(RefreshRequestDTO dto) {
        RefreshToken refreshToken = refreshTokenService.validarEBuscar(dto.refreshToken());
        refreshTokenService.revogarPorUsuario(refreshToken.getUsuario());
    }

    @Transactional
    public void registrar(RegisterRequestDTO dto) {
        if (usuarioRepository.existsByUsername(dto.username())) {
            throw new BusinessException("Username '" + dto.username() + "' já está em uso.");
        }

        Set<String> roles = (dto.roles() == null || dto.roles().isEmpty())
                ? Set.of("USER")
                : dto.roles();

        Usuario usuario = Usuario.builder()
                .username(dto.username())
                .password(passwordEncoder.encode(dto.password()))
                .roles(roles)
                .ativo(true)
                .build();

        usuarioRepository.save(usuario);
    }
}
