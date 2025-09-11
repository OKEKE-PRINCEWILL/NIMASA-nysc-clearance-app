package com.example.NIMASA.NYSC.Clearance.Form.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final UserDetailsService userDetailsService;
    private final JWTFilter jwtFilter;
    private final CorsConfigurationSource corsConfigurationSource; // Inject the CORS config

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource)) // Enable CORS
                .csrf(customizer -> customizer.disable())
                .authorizeHttpRequests(request ->
                        request

                                .requestMatchers("/api/auth/**").permitAll()
                                .requestMatchers("/api/unified-auth/login").permitAll()

                                .requestMatchers(HttpMethod.GET, "/api/clearance-forms").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/clearance-forms/*").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/clearance-forms/search/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/clearance-forms/status/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/clearance-forms/count/**").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/clearance-forms").permitAll()

                                // Review endpoints - no token required (role-based access handled in service)
                                .requestMatchers(HttpMethod.POST, "/api/clearance-forms/*/supervisor-review").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/clearance-forms/*/hod-review").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/clearance-forms/pending").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/clearance-forms/pending/count").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/clearance-forms/*/exists").permitAll()

                                // Admin-only endpoints token will be required
                                .requestMatchers(HttpMethod.POST, "/api/clearance-forms/*/approve").authenticated()
                                .requestMatchers(HttpMethod.POST, "/api/clearance-forms/*/reject").authenticated()
                                .requestMatchers(HttpMethod.DELETE, "/api/clearance-forms/**").authenticated()
                                .requestMatchers(HttpMethod.POST, "/api/clearance-forms/admin/**").authenticated()

                                // Employee management endpoints token will be required
                                .requestMatchers("/api/unified-auth/employee/**").authenticated()

                                // Swagger endpoints
                                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                                // Handle preflight OPTIONS requests
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                .anyRequest().permitAll())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(encoder());
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    @Bean
    public BCryptPasswordEncoder encoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}