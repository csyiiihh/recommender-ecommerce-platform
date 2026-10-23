package org.example.springboot.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.example.springboot.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.Date;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class JwtTokenUtils {
    private static Algorithm algorithm;
    public JwtTokenUtils(@Value("${JWT_SECRET:${jwt.secret}}") String secret) {
        if (secret == null || secret.length() < 32) throw new IllegalStateException("JWT_SECRET must contain at least 32 characters");
        algorithm = Algorithm.HMAC256(secret);
    }
    public static String fingerprint(String password) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    public static String genToken(String userId, String password) {
        return JWT.create().withIssuer("online-sales").withAudience(userId)
            .withClaim("credential", fingerprint(password)).withIssuedAt(new Date())
            .withExpiresAt(Date.from(Instant.now().plusSeconds(7200))).sign(algorithm);
    }
    public static DecodedJWT verify(String token) {
        return JWT.require(algorithm).withIssuer("online-sales").build().verify(token);
    }
    public static User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof User user ? user : null;
    }
}
