package com.codegroup.portfolio.repository;

import com.codegroup.portfolio.entity.RefreshToken;
import com.codegroup.portfolio.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revogado = true WHERE rt.usuario = :usuario")
    void revogarTodosPorUsuario(Usuario usuario);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now OR rt.revogado = true")
    void deletarExpiradosERevogados(Instant now);
}
