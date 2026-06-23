package com.loadfilesservice.loadfiles.automatedtest.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Generador de tokens JWT para pruebas funcionales Karate.
 * Lee el secreto exclusivamente desde la propiedad de sistema 'karate.jwt.secret'
 * para no hardcodear credenciales en el código fuente.
 */
public final class JwtTestUtil {

    private static final long EXPIRY_MS = 3_600_000L;          // 1 hora
    private static final long EXPIRED_OFFSET_MS = 3_700_000L;  // ya expirado

    private JwtTestUtil() {
    }

    /** Genera un token JWT válido con el rol indicado. */
    public static String generateToken(String username, String rol) {
        SecretKey key = resolveKey();
        return Jwts.builder()
                .subject(username)
                .claim("rol", rol)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRY_MS))
                .signWith(key)
                .compact();
    }

    /** Genera un token JWT ya expirado (para pruebas negativas). */
    public static String generateExpiredToken(String username, String rol) {
        SecretKey key = resolveKey();
        Date past = new Date(System.currentTimeMillis() - EXPIRED_OFFSET_MS);
        return Jwts.builder()
                .subject(username)
                .claim("rol", rol)
                .issuedAt(past)
                .expiration(past)
                .signWith(key)
                .compact();
    }

    /** Token con firma inválida (secreto distinto). */
    public static String generateInvalidSignatureToken(String username, String rol) {
        byte[] fakeKey = new byte[64];
        java.util.Arrays.fill(fakeKey, (byte) 0x42);
        SecretKey wrongKey = Keys.hmacShaKeyFor(fakeKey);
        return Jwts.builder()
                .subject(username)
                .claim("rol", rol)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRY_MS))
                .signWith(wrongKey)
                .compact();
    }

    private static SecretKey resolveKey() {
        String secret = System.getProperty("karate.jwt.secret");
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "La propiedad 'karate.jwt.secret' no está configurada. "
                    + "Defínela en src/automated-test/resources/karate.properties "
                    + "o pásala con -Pkarate.jwt.secret=<valor>.");
        }
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}