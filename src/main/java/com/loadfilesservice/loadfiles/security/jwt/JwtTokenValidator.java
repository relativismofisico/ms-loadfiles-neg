package com.loadfilesservice.loadfiles.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
@Slf4j
public class JwtTokenValidator {

    private final SecretKey secretKey;

    public JwtTokenValidator(@Value("${security.jwt.secret}") String secret) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractRol(String token) {
        return (String) extractClaims(token).get("rol");
    }

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