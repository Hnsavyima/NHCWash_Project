package com.nhcwash.backend.configs;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import com.nhcwash.backend.models.entities.User;
import com.nhcwash.backend.repositories.UserRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AuthTokenFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    @Autowired
    private UserRepository userRepository;

    /**
     * Do not parse JWT for password-reset endpoints (public, no Authorization header).
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && uri.startsWith(context)) {
            uri = uri.substring(context.length());
        }
        if (uri.startsWith("/api/auth/password-reset/")) {
            return true;
        }
        // OpenAPI + Swagger UI — no JWT; avoids touching SecurityContext for public doc endpoints
        return uri.startsWith("/v3/api-docs")
                || uri.startsWith("/swagger-ui")
                || "/swagger-ui.html".equals(uri)
                || uri.startsWith("/webjars/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUserNameFromJwtToken(jwt);
                if (username == null || username.isBlank()) {
                    filterChain.doFilter(request, response);
                    return;
                }
                User dbUser = userRepository.findByEmail(username.trim()).orElse(null);
                if (dbUser == null || isSoftDeleted(dbUser)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                List<GrantedAuthority> authorities = jwtUtils.getAuthoritiesFromJwtToken(jwt);
                if (authorities == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    authorities = new ArrayList<>(userDetails.getAuthorities());
                }
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("DEBUG JWT - User: " + username + " | Granted Authorities: " + authorities);
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e);
        }
        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }

    private static boolean isSoftDeleted(User u) {
        return u.getDeletedAt() != null || Boolean.TRUE.equals(u.getIsDeleted());
    }
}