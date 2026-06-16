package com.codegroup.portfolio.config;

import com.codegroup.portfolio.entity.Usuario;
import com.codegroup.portfolio.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initUsuarios() {
        return args -> {
            if (!usuarioRepository.existsByUsername("admin")) {
                Set<String> rolesAdmin = new HashSet<>();
                rolesAdmin.add("ADMIN");
                rolesAdmin.add("USER");

                Usuario admin = new Usuario();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRoles(rolesAdmin);
                admin.setAtivo(true);

                usuarioRepository.save(admin);
                log.info("Usuário 'admin' criado com sucesso.");
            }

            if (!usuarioRepository.existsByUsername("user")) {
                Set<String> rolesUser = new HashSet<>();
                rolesUser.add("USER");

                Usuario user = new Usuario();
                user.setUsername("user");
                user.setPassword(passwordEncoder.encode("user123"));
                user.setRoles(rolesUser);
                user.setAtivo(true);

                usuarioRepository.save(user);
                log.info("Usuário 'user' criado com sucesso.");
            }
        };
    }
}