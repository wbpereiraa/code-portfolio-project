package com.codegroup.portfolio.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * Serviço responsável por gerar, validar e extrair dados dos JWTs.
 * Utiliza HMAC-SHA256 com chave configurada via application.properties.
 */
@Service
@Slf4j
public class JwtService {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-ms}") long accessTokenExpirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    public String gerarAccessToken(UserDetails userDetails) {
        return gerarAccessToken(Map.of(), userDetails);
    }

    public String gerarAccessToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .claim("roles", userDetails.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .toList())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpirationMs))
                .signWith(secretKey)
                .compact();
    }

    public String extrairUsername(String token) {
        return extrairClaim(token, Claims::getSubject);
    }

    public boolean isTokenValido(String token, UserDetails userDetails) {
        try {
            String username = extrairUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpirado(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT inválido: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpirado(String token) {
        return extrairExpiration(token).before(new Date());
    }

    private Date extrairExpiration(String token) {
        return extrairClaim(token, Claims::getExpiration);
    }

    public <T> T extrairClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extrairTodosClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extrairTodosClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }
}
