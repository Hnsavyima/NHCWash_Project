package com.nhcwash.backend.configs;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nhcwash.backend.models.constants.RoleNames;

import java.util.Arrays;
import java.util.List;

@Configuration
public class WebSecurityConfig {

    /** Paths under {@code /api/orders} — allow Spring-style {@code ROLE_*} and plain names from JWT / DB. */
    private static final String[] ORDER_ENDPOINT_AUTHORITIES = {
            RoleNames.CLIENT, "CLIENT",
            RoleNames.ADMIN, "ADMIN",
            RoleNames.EMPLOYEE, "EMPLOYEE",
    };

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:5174",
                "http://127.0.0.1:5174"));
        // Include PATCH explicitly — browsers send Access-Control-Request-Method: PATCH on preflight.
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // CSRF is disabled for this stateless API. CORS allows DELETE (see corsConfigurationSource + CorsConfig).
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // springdoc: include base + yaml + webjars ("/v3/api-docs/**" does not match "/v3/api-docs.yaml")
                        .requestMatchers(
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/webjars/**")
                        .permitAll()
                        // Must be first among remaining rules: specific path before any broader /api/users/** style matchers.
                        .requestMatchers(HttpMethod.DELETE, "/api/users/me")
                                .hasAnyAuthority("ROLE_CLIENT", "CLIENT")
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/password-reset/request",
                                "/api/auth/password-reset/confirm",
                                "/api/contact")
                        .permitAll()
                        .requestMatchers("/api/auth/**").permitAll() // Login, register, password reset — publics
                        .requestMatchers(HttpMethod.GET, "/api/public/settings", "/api/public/site-languages").permitAll()
                        .requestMatchers("/api/services/**").permitAll() // Catalogue public
                        // Invoice PDF — explicit before generic /api/orders/** (clients + staff).
                        .requestMatchers(HttpMethod.GET, "/api/orders/*/invoice")
                                .hasAnyAuthority(ORDER_ENDPOINT_AUTHORITIES)
                        .requestMatchers("/api/orders", "/api/orders/**")
                                .hasAnyAuthority(ORDER_ENDPOINT_AUTHORITIES)
                        .requestMatchers(HttpMethod.POST, "/api/admin/orders/*/mark-as-paid")
                                .hasAnyAuthority(RoleNames.ADMIN, RoleNames.EMPLOYEE)
                        .requestMatchers(HttpMethod.POST, "/api/admin/orders/*/refund")
                                .hasAnyAuthority(RoleNames.ADMIN, RoleNames.EMPLOYEE)
                        .requestMatchers(HttpMethod.GET, "/api/admin/services", "/api/admin/services/**")
                                .hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_Admin", "Admin")
                        // Broad admin API (after specific /api/admin/orders/* and admin services GET rules above).
                        .requestMatchers("/api/admin/**").hasAnyAuthority(RoleNames.ADMIN, "ADMIN")
                        .requestMatchers("/api/employee/**")
                                .hasAnyAuthority(RoleNames.ADMIN, "ADMIN", RoleNames.EMPLOYEE, "EMPLOYEE")
                        .anyRequest().authenticated());

        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
