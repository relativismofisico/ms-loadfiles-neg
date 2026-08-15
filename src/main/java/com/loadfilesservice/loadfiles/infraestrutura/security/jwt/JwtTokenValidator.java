package com.loadfilesservice.loadfiles.infraestrutura.security.jwt;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Componente que valida y parsea tokens JWT. */
@Component
@Slf4j
public class JwtTokenValidator {

    private static final String REFRESH_TYP = "refresh";

    private final SecretKey secretKey;

    /** Inicializa el validador con la clave secreta configurada. */
    public JwtTokenValidator(@Value("${security.jwt.secret}") String secret) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /** Extrae y verifica los claims del token. */
    public Claims extractClaims(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(this.secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // Un refresh token (emitido por ms-security-util, mismo secreto, mismos claims que
        // un access token) nunca es válido como credencial de acceso normal — rechazar SOLO
        // si el claim está presente y vale "refresh": los tokens emitidos antes de que
        // existiera ese claim siguen validando igual, retrocompatible. El filtro captura
        // JwtException/Exception genérico y responde 401.
        boolean isRefreshToken = REFRESH_TYP.equals(claims.get("typ"));
        if (isRefreshToken) {
            log.warn("[JwtTokenValidator] Token de tipo refresh usado como credencial de acceso");
            throw new JwtException("El token es un refresh token, no puede usarse como credencial de acceso.");
        }

        return claims;
    }

    /** Extrae el nombre de usuario del token. */
    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    /** Extrae el rol del token. */
    public String extractRol(String token) {
        return (String) extractClaims(token).get("rol");
    }

    /** Valida el token lanzando excepción si no es válido. */
    public void validateToken(String token) {
        try {
            extractClaims(token);
        } catch (ExpiredJwtException e) {
            log.warn("[JwtTokenValidator] Token expirado");
            throw new ExpiredJwtException(e.getHeader(), e.getClaims(), "Token JWT expirado");
        } catch (SignatureException e) {
            log.warn("[JwtTokenValidator] Firma JWT inválida");
            throw new SignatureException("Firma JWT inválida");
        } catch (MalformedJwtException e) {
            log.warn("[JwtTokenValidator] Token JWT malformado");
            throw new MalformedJwtException("Token JWT malformado");
        } catch (UnsupportedJwtException e) {
            log.warn("[JwtTokenValidator] Token JWT no soportado");
            throw new UnsupportedJwtException("Token JWT no soportado");
        }
    }
}
