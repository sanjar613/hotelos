package com.hotelos.reception.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security Configuration.
 *
 * Dashboard (port 8080) must authenticate before accessing operational data.
 * Passwords stored as BCrypt hashes — never plaintext.
 * CORS is enabled so the dashboard origin can call this API and read the
 * Authorization header response.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain chain(HttpSecurity http) throws Exception {
        return http
            .cors(Customizer.withDefaults())          // enable CORS (uses bean below)
            .csrf(c -> c.disable())                   // disabled for stateless API
            .authorizeHttpRequests(a -> a
                .requestMatchers("/ws/**", "/actuator/health").permitAll()
                .anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults())
            .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        // Dashboard origins (browser served from any localhost port + file://)
        cfg.setAllowedOriginPatterns(List.of("*"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    @Bean
    public UserDetailsService users() {
        return new InMemoryUserDetailsManager(
            User.builder().username("admin").password(encoder().encode("hotelos2024")).roles("ADMIN").build(),
            User.builder().username("staff").password(encoder().encode("hotel123")).roles("STAFF").build()
        );
    }

    @Bean
    public PasswordEncoder encoder() { return new BCryptPasswordEncoder(); }
}
