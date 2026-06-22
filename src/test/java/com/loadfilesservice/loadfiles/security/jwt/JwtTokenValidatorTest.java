package com.loadfilesservice.loadfiles.security.jwt;

import com.loadfilesservice.loadfiles.infraestrutura.security.jwt.JwtTokenValidator;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenValidatorTest {

    private static final String SECRET = "AF84F1FGllNpNnLG055fdg5hGHJK4KGG5VH5TR5J05JFGGDFDGXVV545J4505G666JFGF2mMY95y";

    private JwtTokenValidator validator;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        validator = new JwtTokenValidator(SECRET);
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    private String buildToken(String subject, String rol, long expirationMs) {
        Map<String, Object> claims = new HashMap<>();
        if (rol != null) claims.put("rol", rol);
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    @Test
    void extractUsername_shouldReturnSubject() {
        String token = buildToken("usuario@test.com", "ADMINISTRADOR", 86400000L);
        assertThat(validator.extractUsername(token)).isEqualTo("usuario@test.com");
    }

    @Test
    void extractRol_shouldReturnRolClaim() {
        String token = buildToken("usuario@test.com", "EMPRESA", 86400000L);
        assertThat(validator.extractRol(token)).isEqualTo("EMPRESA");
    }

    @Test
    void validateToken_shouldNotThrow_whenTokenIsValid() {
        String token = buildToken("usuario@test.com", "OPERARIO", 86400000L);
        validator.validateToken(token);
    }

    @Test
    void validateToken_shouldThrowExpiredJwtException_whenTokenIsExpired() {
        String token = buildToken("usuario@test.com", "FONDEADOR", -1000L);
        assertThatThrownBy(() -> validator.validateToken(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void validateToken_shouldThrow_whenTokenHasInvalidSignature() {
        // Token firmado con clave distinta (256-bit válida para HMAC-SHA)
        String otherSecret = "QUY4NEYxRkdsbE5wTm5MRzA1NWZkZzVoR0hKSzRLR0dHSEhISklJS0tMTE1NTk5P";
        SecretKey otherKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(otherSecret));
        String tokenWithWrongSecret = Jwts.builder()
                .subject("user@test.com")
                .expiration(new Date(System.currentTimeMillis() + 86400000L))
                .signWith(otherKey)
                .compact();

        assertThatThrownBy(() -> validator.validateToken(tokenWithWrongSecret))
                .isInstanceOf(Exception.class);
    }

    @Test
    void extractClaims_shouldReturnAllClaims() {
        String token = buildToken("test@test.com", "ADMINISTRADOR", 86400000L);
        var claims = validator.extractClaims(token);
        assertThat(claims.getSubject()).isEqualTo("test@test.com");
        assertThat(claims.get("rol")).isEqualTo("ADMINISTRADOR");
    }
}