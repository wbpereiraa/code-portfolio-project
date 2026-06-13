package com.codegroup.portfolio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Segurança básica com usuário/senha em memória, conforme exigido pelo desafio.
 *
 * Usuário: admin / senha: admin123  -> ROLE_ADMIN (acesso total)
 * Usuário: user  / senha: user123   -> ROLE_USER  (somente leitura)
 *
 * Endpoints do Swagger, da API mockada externa e do H2 (se usado) ficam liberados.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        var admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .roles("ADMIN", "USER")
                .build();

        var user = User.builder()
                .username("user")
                .password(passwordEncoder.encode("user123"))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(admin, user);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Documentação e API externa mockada liberadas
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/external/**"
                        ).permitAll()
                        // Leitura liberada para USER e ADMIN
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/**").hasAnyRole("USER", "ADMIN")
                        // Escrita restrita ao ADMIN
                        .anyRequest().hasRole("ADMIN")
                )
                .httpBasic(org.springframework.security.config.Customizer.withDefaults());

        return http.build();
    }
}
