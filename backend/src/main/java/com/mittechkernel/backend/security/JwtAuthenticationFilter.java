package com.mittechkernel.backend.security;

import com.mittechkernel.backend.common.security.CurrentUser;
import com.mittechkernel.backend.modules.auth.repository.AuthUserRepository;
import com.mittechkernel.backend.modules.auth.service.JwtService;
import com.mittechkernel.backend.modules.auth.entity.AuthUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthUserRepository authUserRepository;

    public JwtAuthenticationFilter(JwtService jwtService, AuthUserRepository authUserRepository) {
        this.jwtService = jwtService;
        this.authUserRepository = authUserRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7).trim();
            try {
                jwtService.validateAccessToken(token);
                Long userId = jwtService.extractUserId(token);
                AuthUser account = userId == null ? null
                        : authUserRepository.findByIdAndActiveTrue(userId).orElse(null);
                if (account != null && jwtService.extractAuthVersion(token) == account.getAuthVersion()) {
                    Set<String> roles = account.getRoles();
                    CurrentUser principal = new CurrentUser(account.getId(), account.getEmail(), roles);
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).collect(Collectors.toSet())
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (RuntimeException ignored) {
                // Invalid, expired, or non-access tokens remain unauthenticated.
            }
        }

        filterChain.doFilter(request, response);
    }
}
