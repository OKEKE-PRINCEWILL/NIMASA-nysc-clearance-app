
package com.example.NIMASA.NYSC.Clearance.Form.SecurityService;

import com.example.NIMASA.NYSC.Clearance.Form.model.RefreshToken;
import com.example.NIMASA.NYSC.Clearance.Form.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Create a new refresh token for a user
     */
    @Transactional
    public RefreshToken createRefreshToken(String employeeName, String tokenFamily, String deviceInfo) {
        // Generate secure random token
        String rawToken = jwtService.generateSecureRandomToken();

        // Hash the token before storing (security best practice)
        String hashedToken = passwordEncoder.encode(rawToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(hashedToken);
        refreshToken.setEmployeeName(employeeName);
        refreshToken.setTokenFamily(tokenFamily);
        refreshToken.setDeviceInfo(deviceInfo);
        refreshToken.setExpirationDate(LocalDateTime.now().plusSeconds(jwtService.getRefreshTokenExpirationMs() / 1000));
        refreshToken.setCreatedAt(LocalDateTime.now());

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * OPTIMIZED: Fast token validation using JWT structure first
     */
    public Optional<String> validateRefreshToken(String rawToken) {
        try {
            // First, validate JWT structure (fast)
            if (!jwtService.validateRefreshTokenStructure(rawToken)) {
                return Optional.empty();
            }

            // Extract username from JWT (fast)
            String username = jwtService.extractUsername(rawToken);
            String tokenFamily = jwtService.extractTokenFamily(rawToken);

            // Only query database for tokens matching this user and family (much smaller dataset)
            List<RefreshToken> candidateTokens = refreshTokenRepository
                    .findByEmployeeNameAndTokenFamilyAndRevokedFalse(username, tokenFamily);

            // Check only relevant tokens with BCrypt (minimal BCrypt operations)
            for (RefreshToken token : candidateTokens) {
                if (!token.isExpired() && passwordEncoder.matches(rawToken, token.getToken())) {
                    return Optional.of(token.getEmployeeName());
                }
            }
        } catch (Exception e) {
            // Invalid JWT structure or other error
            return Optional.empty();
        }

        return Optional.empty();
    }

    /**
     * OPTIMIZED: Fast token lookup for logout
     */
    private Optional<RefreshToken> findTokenByRawValueOptimized(String rawToken) {
        try {
            // Extract info from JWT first (fast)
            String username = jwtService.extractUsername(rawToken);
            String tokenFamily = jwtService.extractTokenFamily(rawToken);

            // Query only relevant tokens
            List<RefreshToken> candidateTokens = refreshTokenRepository
                    .findByEmployeeNameAndTokenFamilyAndRevokedFalse(username, tokenFamily);

            // Check only a few tokens instead of all tokens
            for (RefreshToken token : candidateTokens) {
                if (passwordEncoder.matches(rawToken, token.getToken())) {
                    return Optional.of(token);
                }
            }
        } catch (Exception e) {
            // Invalid JWT structure
            return Optional.empty();
        }

        return Optional.empty();
    }

    /**
     * OPTIMIZED: Rotate refresh token with fast lookup
     */
    @Transactional
    public RefreshToken rotateRefreshToken(String oldRawToken, String deviceInfo) {
        // Find and validate the old token (now optimized)
        Optional<RefreshToken> oldTokenOpt = findTokenByRawValueOptimized(oldRawToken);

        if (oldTokenOpt.isEmpty() || oldTokenOpt.get().isRevoked() || oldTokenOpt.get().isExpired()) {
            // If token is invalid, revoke entire family (security measure)
            if (oldTokenOpt.isPresent()) {
                revokeTokenFamily(oldTokenOpt.get().getTokenFamily());
            }
            throw new RuntimeException("Invalid refresh token");
        }

        RefreshToken oldToken = oldTokenOpt.get();

        // Revoke the old token
        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);

        // Create new token with same family
        return createRefreshToken(oldToken.getEmployeeName(), oldToken.getTokenFamily(), deviceInfo);
    }

    /**
     * OPTIMIZED: Fast logout for single session
     */
    @Transactional
    public int revokeSingleSession(String refreshToken) {
        try {
            // Extract token family from JWT (fast)
            String tokenFamily = jwtService.extractTokenFamily(refreshToken);

            // Revoke just this token family (much faster than checking all tokens)
            int revokedCount = refreshTokenRepository.revokeTokenFamily(tokenFamily);
            return revokedCount > 0 ? 1 : 0;

        } catch (Exception e) {
            // If JWT parsing fails, fall back to slow method (rare case)
            return revokeSingleSessionFallback(refreshToken);
        }
    }

    /**
     * Fallback method for single session logout (rare case)
     */
    @Transactional
    private int revokeSingleSessionFallback(String refreshToken) {
        Optional<RefreshToken> tokenOpt = findTokenByRawValueOptimized(refreshToken);
        if (tokenOpt.isPresent()) {
            refreshTokenRepository.revokeTokenFamily(tokenOpt.get().getTokenFamily());
            return 1;
        }
        return 0;
    }

    /**
     * Revoke all tokens for a user (logout all devices) - already optimized
     */
    @Transactional
    public int revokeAllTokensForEmployee(String employeeName) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findByEmployeeNameAndRevokedFalse(employeeName);
        refreshTokenRepository.revokeAllTokensForEmployee(employeeName);
        return activeTokens.size();
    }

    /**
     * Revoke a specific token family (when compromise detected) - already optimized
     */
    @Transactional
    public void revokeTokenFamily(String tokenFamily) {
        refreshTokenRepository.revokeTokenFamily(tokenFamily);
    }

    /**
     * Get active session count for a user - already optimized
     */
    public long getActiveSessionCount(String employeeName) {
        return refreshTokenRepository.countActiveSessionsForEmployee(employeeName, LocalDateTime.now());
    }

    /**
     * Clean up expired tokens - runs every hour - already optimized
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            refreshTokenRepository.deleteExpiredAndRevokedTokens(LocalDateTime.now());
            System.out.println("Cleaned up expired refresh tokens at: " + LocalDateTime.now());
        } catch (Exception e) {
            System.err.println("Failed to clean up expired tokens: " + e.getMessage());
        }
    }
}