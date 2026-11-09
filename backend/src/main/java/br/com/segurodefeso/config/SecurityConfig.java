package br.com.segurodefeso.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import br.com.segurodefeso.repositorio.UsuarioRepo;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filtro(HttpSecurity http, SecurityContextRepository contexto) throws Exception {
        http
            .csrf(c -> c
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
            .securityContext(s -> s.securityContextRepository(contexto))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/auth/login", "/api/auth/csrf").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/honorarios/**", "/api/relatorios/**").hasAnyRole("ADMIN", "ADVOGADO")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll())          // páginas do React
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .formLogin(f -> f.disable())
            .httpBasic(b -> b.disable())
            .logout(l -> l
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)))
            .headers(h -> h
                .frameOptions(f -> f.deny())
                .contentTypeOptions(c -> { })
                .referrerPolicy(r -> r.policy(
                    org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)));
        return http.build();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UsuarioRepo usuarios) {
        return email -> usuarios.findByEmailIgnoreCase(email)
            .map(u -> User.withUsername(u.getEmail())
                .password(u.getSenhaHash())
                .roles(u.getPapel().name())
                .disabled(!u.isAtivo())
                .build())
            .orElseThrow(() -> new UsernameNotFoundException("não encontrado"));
    }

    @Bean
    @SuppressWarnings("deprecation")
    public AuthenticationManager authenticationManager(UserDetailsService uds, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(uds);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }
}
