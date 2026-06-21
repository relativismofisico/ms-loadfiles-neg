package com.loadfilesservice.loadfiles.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loadfilesservice.loadfiles.exceptions.ApiErrorResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenValidator jwtTokenValidator;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String token = extractBearerToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtTokenValidator.extractClaims(token);
            String username = claims.getSubject();
            String rol = (String) claims.get("rol");

            if (username != null && rol != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                var authorities = List.of(new SimpleGrantedAuthority(rol));
                var authentication = new UsernamePasswordAuthenticationToken(username, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

        } catch (ExpiredJwtException e) {
            log.warn("[JwtAuthenticationFilter] Token expirado para request: {}", request.getRequestURI());
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Token JWT expirado", request.getRequestURI());
            return;
        } catch (Exception e) {
            log.warn("[JwtAuthenticationFilter] Token inválido para request {}: {}", request.getRequestURI(), e.getMessage());
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Token JWT inválido", request.getRequestURI());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private void writeErrorResponse(HttpServletResponse response, HttpStatus status,
                                    String message, String path) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                status.value(), status.getReasonPhrase(), message, path);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}