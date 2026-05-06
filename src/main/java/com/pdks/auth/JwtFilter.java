package com.pdks.auth;

import com.pdks.config.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        if (!jwtService.isValid(token)) {
            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Geçersiz token\"}");
            return;
        }

        // Schema'yı ThreadLocal'e yaz (DB yönlendirmesi için)
        String schema = jwtService.extractSchema(token);
        TenantContext.setTenant(schema);

        // tenantId'yi ThreadLocal'e yaz (limit kontrolü + /me endpoint'i için)
        String tenantIdStr = jwtService.extractTenantId(token);
        if (tenantIdStr != null) {
            try {
                TenantContext.setTenantId(UUID.fromString(tenantIdStr));
            } catch (IllegalArgumentException e) {
                log.warn("JWT'deki tenantId geçerli bir UUID değil: {}", tenantIdStr);
            }
        }

        // Spring Security context'ini doldur
        String userId = jwtService.extractUserId(token);
        String role   = jwtService.extractRole(token);

        var auth = new UsernamePasswordAuthenticationToken(
            userId,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            chain.doFilter(request, response);
        } finally {
            // Her istekten sonra ThreadLocal temizlenmeli — memory leak önlemi
            TenantContext.clear();
        }
    }
}
