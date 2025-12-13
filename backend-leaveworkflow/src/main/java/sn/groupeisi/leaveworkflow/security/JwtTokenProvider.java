package sn.groupeisi.leaveworkflow.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        return generateTokenForUsername(username);
    }

    public String generateToken(String username) {
        return generateTokenForUsername(username);
    }

    private String generateTokenForUsername(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + jwtExpiration))
                .signWith(getSignKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String getUserNameFromJwt(String token) {
        try {
            Claims claims = parseClaims(token);
            String username = claims != null ? claims.getSubject() : null;
            System.out.println("Extracted username from JWT: " + username);
            return username;
        } catch (Exception e) {
            System.err.println("ERROR parsing JWT token: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            boolean isValid = claims != null;
            System.out.println("Token validation result: " + isValid);
            if (claims != null) {
                System.out.println("Token subject: " + claims.getSubject());
                System.out.println("Token expiration: " + claims.getExpiration());
            }
            return isValid;
        } catch (Exception e) {
            System.err.println("Token validation failed: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Reflection-based parsing: supports both older jjwt (parser()) and newer (parserBuilder()) APIs
    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            System.err.println("Failed to parse JWT claims: " + e.getMessage());
            throw e;
        }
    }
}