
//}
package com.example.NIMASA.NYSC.Clearance.Form.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final UserDetailsService userDetailsService;
    private final JWTFilter jwtFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS Configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // CSRF - disabled for API (using JWT tokens)
                .csrf(customizer -> customizer.disable())

                // Security Headers
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny()) // Prevent clickjacking
                        .contentTypeOptions(withDefaults())  // X-Content-Type-Options: nosniff
                        .referrerPolicy(ref -> ref
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                        )
                        .httpStrictTransportSecurity(hsts -> hsts
                                .maxAgeInSeconds(31536000) // 1 year
                                .includeSubDomains(true)
                                .preload(true)
                        )
                )


                // Authorization Rules
                .authorizeHttpRequests(request -> request

                        // Public authentication endpoints
                        .requestMatchers("/api/unified-auth/login").permitAll()
                        .requestMatchers("/api/unified-auth/refresh").permitAll()
                        .requestMatchers("/api/unified-auth/bootstrap/**").permitAll()
                        .requestMatchers("/api/unified-auth/rate-limit/status").permitAll()

                        // Public clearance form endpoints (read-only)
                        .requestMatchers(HttpMethod.GET, "/api/clearance-forms").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/clearance-forms/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/clearance-forms/search/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/clearance-forms/status/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/clearance-forms/count/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/clearance-forms/*/exists").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/clearance-forms/signatures/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/clearance-forms/*/printable").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/clearance-forms/approved/corps/**").permitAll()

                        // Public form creation and review (no auth needed for basic operations)
                        .requestMatchers(HttpMethod.POST, "/api/clearance-forms").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/clearance-forms/*/supervisor-review").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/clearance-forms/*/hod-review").permitAll()

                        // Protected employee management endpoints (require authentication)
                        .requestMatchers("/api/unified-auth/employee/**").authenticated()
                        .requestMatchers("/api/unified-auth/session/**").authenticated()
                        .requestMatchers("/api/unified-auth/logout").permitAll() // Allow logout without auth (clear cookies)

                        // Protected admin-only endpoints
                        .requestMatchers(HttpMethod.POST, "/api/clearance-forms/*/approve").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/clearance-forms/*/reject").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/clearance-forms/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/clearance-forms/pending").permitAll() // Allow all to see pending (filtering in service)
                        .requestMatchers(HttpMethod.GET, "/api/clearance-forms/pending/count").permitAll()

                        // Documentation endpoints
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()

                        // CORS preflight requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Default: permit all (your current setup, can be changed to authenticated)
                        .anyRequest().permitAll()
                )

                // Stateless session management (JWT-based)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Authentication provider
                .authenticationProvider(authenticationProvider())

                // JWT Filter
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