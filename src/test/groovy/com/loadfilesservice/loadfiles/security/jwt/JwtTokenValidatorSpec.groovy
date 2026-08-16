package com.loadfilesservice.loadfiles.security.jwt

import com.loadfilesservice.loadfiles.infraestrutura.security.jwt.JwtTokenValidator
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import spock.lang.Specification

import javax.crypto.SecretKey

class JwtTokenValidatorSpec extends Specification {

    static final String SECRET = "AF84F1FGllNpNnLG055fdg5hGHJK4KGG5VH5TR5J05JFGGDFDGXVV545J4505G666JFGF2mMY95y"
    JwtTokenValidator validator = new JwtTokenValidator(SECRET)
    SecretKey secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET))

    String buildToken(String subject, String rol, long expirationMs) {
        Map<String, Object> claims = [:]
        if (rol != null) claims.put("rol", rol)
        Jwts.builder()
            .claims(claims)
            .subject(subject)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(secretKey)
            .compact()
    }

    def "extractUsername - returns subject from token"() {
        given:
        def token = buildToken("usuario@test.com", "ADMINISTRADOR", 86400000L)

        when:
        def result = validator.extractUsername(token)

        then:
        result == "usuario@test.com"
    }

    def "extractRol - returns rol claim from token"() {
        given:
        def token = buildToken("usuario@test.com", "EMPRESA", 86400000L)

        when:
        def result = validator.extractRol(token)

        then:
        result == "EMPRESA"
    }

    def "extractRol - returns null when rol claim is absent"() {
        given:
        def token = Jwts.builder()
            .subject("usuario@test.com")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 86400000L))
            .signWith(secretKey)
            .compact()

        when:
        def result = validator.extractRol(token)

        then:
        result == null
    }

    def "extractClaims - returns all claims"() {
        given:
        def token = buildToken("test@test.com", "OPERARIO", 86400000L)

        when:
        def claims = validator.extractClaims(token)

        then:
        claims.getSubject() == "test@test.com"
        claims.get("rol") == "OPERARIO"
    }

    def "extractClaims - throws JwtException for a refresh token used as access token"() {
        given:
        def token = Jwts.builder()
            .subject("test@test.com")
            .claim("rol", "OPERARIO")
            .claim("typ", "refresh")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 86400000L))
            .signWith(secretKey)
            .compact()

        when:
        validator.extractClaims(token)

        then:
        thrown(JwtException)
    }

    def "extractClaims - accepts a token with typ=access"() {
        given:
        def token = Jwts.builder()
            .subject("test@test.com")
            .claim("rol", "OPERARIO")
            .claim("typ", "access")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 86400000L))
            .signWith(secretKey)
            .compact()

        when:
        def claims = validator.extractClaims(token)

        then:
        claims.getSubject() == "test@test.com"
    }

    def "extractClaims - accepts a legacy token without the typ claim (backward compatibility)"() {
        given:
        def token = buildToken("test@test.com", "OPERARIO", 86400000L)

        when:
        def claims = validator.extractClaims(token)

        then:
        claims.getSubject() == "test@test.com"
    }

    def "validateToken - does not throw for valid token"() {
        given:
        def token = buildToken("usuario@test.com", "OPERARIO", 86400000L)

        when:
        validator.validateToken(token)

        then:
        notThrown(Exception)
    }

    def "validateToken - throws ExpiredJwtException for expired token"() {
        given:
        def token = buildToken("usuario@test.com", "FONDEADOR", -1000L)

        when:
        validator.validateToken(token)

        then:
        thrown(ExpiredJwtException)
    }

    def "validateToken - throws exception for token with invalid signature"() {
        given:
        def otherSecret = "QUY4NEYxRkdsbE5wTm5MRzA1NWZkZzVoR0hKSzRLR0dHSEhISklJS0tMTE1NTk5P"
        def otherKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(otherSecret))
        def token = Jwts.builder()
            .subject("user@test.com")
            .expiration(new Date(System.currentTimeMillis() + 86400000L))
            .signWith(otherKey)
            .compact()

        when:
        validator.validateToken(token)

        then:
        thrown(Exception)
    }

    def "validateToken - throws exception for malformed token"() {
        when:
        validator.validateToken("this.is.not.a.valid.jwt.token")

        then:
        thrown(Exception)
    }
}