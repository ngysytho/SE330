package com.example.chatapp.security;

import com.example.chatapp.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getId())
                .claim("userId", user.getId())
                .claim("firebaseUid", user.getUid())
                .claim("gmail", user.getGmail())
                .claim("name", user.getName())
                .claim("role", user.getRole() == null ? "DEFAULT" : user.getRole().name())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    public CustomUserPrincipal parsePrincipal(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new CustomUserPrincipal(
                claim(claims, "userId", claims.getSubject()),
                claim(claims, "firebaseUid", null),
                claim(claims, "gmail", null),
                claim(claims, "name", null),
                claim(claims, "role", "DEFAULT")
        );
    }

    private String claim(Claims claims, String key, String fallback) {
        Object value = claims.get(key);
        return value == null ? fallback : value.toString();
    }
}
