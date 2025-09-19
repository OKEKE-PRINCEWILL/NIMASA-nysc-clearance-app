
package com.example.NIMASA.NYSC.Clearance.Form.SecurityService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${spring.jwt.secret}")
    private String secretKey;

    // Getters for expiration times (used by other services)
    @Getter
    @Value("${spring.jwt.access-token.expiration}")
    private long accessTokenExpirationMs;

    @Getter
    @Value("${spring.jwt.refresh-token.expiration}")
    private long refreshTokenExpirationMs;

    public JwtService(){
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
            SecretKey sk = keyGen.generateKey();
            Base64.getEncoder().encodeToString((sk.getEncoded()));
        }
        catch (NoSuchAlgorithmException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * Generate short-lived access token (15 minutes)
     * Used for API authentication
     */
    public String generateAccessToken(String username){
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access"); // Token type for validation

        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpirationMs))
                .and()
                .signWith(getKey())
                .compact();
    }

    /**
     * Generate long-lived refresh token (7 days)
     * Used only for getting new access tokens
     */
    public String generateRefreshToken(String username, String tokenFamily){
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh"); // Token type for validation
        claims.put("family", tokenFamily); // Token family for rotation security

        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpirationMs))
                .and()
                .signWith(getKey())
                .compact();
    }

    /**
     * Generate cryptographically secure random token family ID
     */
    public String generateTokenFamily() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate secure random refresh token for database storage
     */
    public String generateSecureRandomToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    public String extractTokenFamily(String token) {
        return extractClaim(token, claims -> claims.get("family", String.class));
    }

    private SecretKey getKey(){
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Validate access token for API requests
     */
    public boolean validateAccessToken(String token, UserDetails userDetails) {
        try {
            final String userName = extractUsername(token);
            final String tokenType = extractTokenType(token);

            return (userName.equals(userDetails.getUsername())
                    && "access".equals(tokenType)
                    && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validate refresh token structure (database validation separate)
     */
    public boolean validateRefreshTokenStructure(String token) {
        try {
            final String tokenType = extractTokenType(token);
            return "refresh".equals(tokenType) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims,T> claimResolver){
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    public long getTokenRemainingTimeMinutes(String token) {
        try {
            Date expiration = extractExpiration(token);
            long remainingMs = expiration.getTime() - System.currentTimeMillis();
            return Math.max(0, remainingMs / (1000 * 60)); // Convert to minutes
        } catch (Exception e) {
            return 0;
        }
    }

}