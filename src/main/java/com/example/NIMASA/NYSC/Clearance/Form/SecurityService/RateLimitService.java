package com.example.NIMASA.NYSC.Clearance.Form.SecurityService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class RateLimitService {

    @Value("${security.rate-limit.login.max-attempts:5}")
    private int maxLoginAttempts;

    @Value("${security.rate-limit.login.window-minutes:15}")
    private int timeWindowMinutes;

    // In-memory storage for rate limiting (in production, use Redis)
    private final Map<String, LoginAttemptRecord> loginAttempts = new ConcurrentHashMap<>();

    /**
     * Check if login is allowed for this IP
     */
    public boolean isLoginAllowed(String clientIp) {
        LoginAttemptRecord record = loginAttempts.get(clientIp);

        if (record == null) {
            return true; // No previous attempts
        }

        // Clean up old attempts outside the time window
        cleanupOldAttempts(record);

        return record.getFailedAttempts() < maxLoginAttempts;
    }

    /**
     * Record a failed login attempt
     */
    public void recordFailedLogin(String clientIp) {
        LoginAttemptRecord record = loginAttempts.computeIfAbsent(clientIp, k -> new LoginAttemptRecord());
        record.addFailedAttempt();
    }

    /**
     * Record a successful login (resets failed attempts)
     */
    public void recordSuccessfulLogin(String clientIp) {
        loginAttempts.remove(clientIp); // Reset on successful login
    }

    /**
     * Get remaining attempts for an IP
     */
    public int getRemainingAttempts(String clientIp) {
        LoginAttemptRecord record = loginAttempts.get(clientIp);
        if (record == null) {
            return maxLoginAttempts;
        }

        cleanupOldAttempts(record);
        return Math.max(0, maxLoginAttempts - record.getFailedAttempts());
    }

    /**
     * Get time until next attempt is allowed
     */
    public long getTimeUntilNextAttemptMinutes(String clientIp) {
        LoginAttemptRecord record = loginAttempts.get(clientIp);
        if (record == null || record.getFailedAttempts() < maxLoginAttempts) {
            return 0;
        }

        LocalDateTime oldestAttempt = record.getOldestAttemptTime();
        if (oldestAttempt == null) {
            return 0;
        }

        LocalDateTime nextAllowedTime = oldestAttempt.plus(timeWindowMinutes, ChronoUnit.MINUTES);
        return ChronoUnit.MINUTES.between(LocalDateTime.now(), nextAllowedTime);
    }

    /**
     * Clean up attempts older than the time window
     */
    private void cleanupOldAttempts(LoginAttemptRecord record) {
        LocalDateTime cutoffTime = LocalDateTime.now().minus(timeWindowMinutes, ChronoUnit.MINUTES);
        record.removeOldAttempts(cutoffTime);
    }

    /**
     * Manual cleanup - remove records with no recent attempts
     */
    public void cleanupStaleRecords() {
        LocalDateTime cutoffTime = LocalDateTime.now().minus(timeWindowMinutes * 2L, ChronoUnit.MINUTES);
        loginAttempts.entrySet().removeIf(entry -> {
            LoginAttemptRecord record = entry.getValue();
            return record.getLastAttemptTime().isBefore(cutoffTime);
        });
    }

    /**
     * Inner class to track login attempts for an IP
     */
    private static class LoginAttemptRecord {
        private final ConcurrentHashMap<LocalDateTime, Boolean> attempts = new ConcurrentHashMap<>();
        private volatile LocalDateTime lastAttemptTime = LocalDateTime.now();

        public void addFailedAttempt() {
            LocalDateTime now = LocalDateTime.now();
            attempts.put(now, Boolean.FALSE);
            lastAttemptTime = now;
        }

        public int getFailedAttempts() {
            return attempts.size();
        }

        public LocalDateTime getLastAttemptTime() {
            return lastAttemptTime;
        }

        public LocalDateTime getOldestAttemptTime() {
            return attempts.keySet().stream().min(LocalDateTime::compareTo).orElse(null);
        }

        public void removeOldAttempts(LocalDateTime cutoffTime) {
            attempts.entrySet().removeIf(entry -> entry.getKey().isBefore(cutoffTime));
        }
    }
}