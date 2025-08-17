package com.example.NIMASA.NYSC.Clearance.Form.Security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final UserDetailsService userDetailsService;
    private final JWTFilter jwtFilter;
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(customizer -> customizer.disable())
                .authorizeHttpRequests(request ->
                        request
                                // Authentication endpoints - no token required
                                .requestMatchers("/api/auth/**").permitAll()

                                // Public clearance form endpoints - no token required
                                .requestMatchers(HttpMethod.GET, "/api/clearance-forms").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/clearance-forms/*").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/clearance-forms/search/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/clearance-forms/status/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/clearance-forms/count/**").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/clearance-forms").permitAll()

                                // Supervisor and HOD review endpoints - no token required
                                .requestMatchers(HttpMethod.POST, "/api/clearance-forms/*/supervisor-review").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/clearance-forms/*/hod-review").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/clearance-forms/supervisor/pending").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/clearance-forms/hod/pending").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/clearance-forms/*/exists").permitAll()

                                // Admin-only endpoints - token required
                                .requestMatchers(HttpMethod.POST, "/api/clearance-forms/*/approve").authenticated()
                                .requestMatchers(HttpMethod.POST, "/api/clearance-forms/*/reject").authenticated()
                                .requestMatchers(HttpMethod.DELETE, "/api/clearance-forms/**").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/clearance-forms/admin/pending").authenticated()
                                .requestMatchers(HttpMethod.POST, "/api/clearance-forms/admin/**").authenticated()

                                // Any other request - allow without authentication
                                .anyRequest().permitAll())
                //.httpBasic(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);


        return http.build();
    }
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(encoder()); //NoOpPasswordEncoder.getInstance());// new BCryptPasswordEncoder(12));
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }
    @Bean
    public BCryptPasswordEncoder encoder(){
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}