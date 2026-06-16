package com.codegroup.portfolio.service;

import com.codegroup.portfolio.entity.Usuario;
import com.codegroup.portfolio.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    private Usuario buildUsuario(String username, boolean ativo, String... roles) {
        Set<String> roleSet = new HashSet<>();
        for (String r : roles) roleSet.add(r);

        Usuario u = new Usuario();
        u.setId(1L);
        u.setUsername(username);
        u.setPassword("encoded");
        u.setRoles(roleSet);
        u.setAtivo(ativo);
        return u;
    }

    @Test
    void deveCarregarUsuarioPorUsernameComSucesso() {
        when(usuarioRepository.findByUsername("admin"))
                .thenReturn(Optional.of(buildUsuario("admin", true, "ADMIN", "USER")));

        UserDetails details = service.loadUserByUsername("admin");

        assertThat(details.getUsername()).isEqualTo("admin");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
    }

    @Test
    void deveRetornarUsuarioDesabilitadoQuandoAtivoFalse() {
        when(usuarioRepository.findByUsername("inativo"))
                .thenReturn(Optional.of(buildUsuario("inativo", false, "USER")));

        UserDetails details = service.loadUserByUsername("inativo");

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoEncontrado() {
        when(usuarioRepository.findByUsername("desconhecido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("desconhecido"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("desconhecido");
    }

    @Test
    void deveMapearRolesCorretamenteParaAuthorities() {
        when(usuarioRepository.findByUsername("user"))
                .thenReturn(Optional.of(buildUsuario("user", true, "USER")));

        UserDetails details = service.loadUserByUsername("user");

        assertThat(details.getAuthorities()).hasSize(1);
        assertThat(details.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }
}
