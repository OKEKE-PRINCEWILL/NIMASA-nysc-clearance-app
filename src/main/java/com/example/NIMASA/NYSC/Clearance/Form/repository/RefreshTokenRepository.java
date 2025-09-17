//package com.example.NIMASA.NYSC.Clearance.Form.repository;
//
//import com.example.NIMASA.NYSC.Clearance.Form.model.RefreshToken;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Modifying;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//
//public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
//
//    // Find valid refresh token
//    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);
//
//    // Find all tokens for a user (for logout all devices)
//    List<RefreshToken> findByEmployeeNameAndRevokedFalse(String employeeName);
//
//    // Find all tokens in a family (for token rotation security)
//    List<RefreshToken> findByTokenFamily(String tokenFamily);
//
//    // Revoke all tokens for a user
//    @Modifying
//    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.employeeName = :employeeName")
//    void revokeAllTokensForEmployee(@Param("employeeName") String employeeName);
//
//    // Revoke all tokens in a family (when one is compromised)
//    @Modifying
//    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.tokenFamily = :tokenFamily")
//    void revokeTokenFamily(@Param("tokenFamily") String tokenFamily);
//
//    // Clean up expired tokens (scheduled job)
//    @Modifying
//    @Query("DELETE FROM RefreshToken rt WHERE rt.expirationDate < :now OR rt.revoked = true")
//    void deleteExpiredAndRevokedTokens(@Param("now") LocalDateTime now);
//
//    // Count active sessions for a user
//    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.employeeName = :employeeName AND rt.revoked = false AND rt.expirationDate > :now")
//    long countActiveSessionsForEmployee(@Param("employeeName") String employeeName, @Param("now") LocalDateTime now);
//}
package com.example.NIMASA.NYSC.Clearance.Form.repository;

import com.example.NIMASA.NYSC.Clearance.Form.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // Find valid refresh token
    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    // Find all tokens for a user (for logout all devices)
    List<RefreshToken> findByEmployeeNameAndRevokedFalse(String employeeName);

    // NEW: Optimized query for fast token lookup by user and family
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.employeeName = :employeeName AND rt.tokenFamily = :tokenFamily AND rt.revoked = false")
    List<RefreshToken> findByEmployeeNameAndTokenFamilyAndRevokedFalse(@Param("employeeName") String employeeName, @Param("tokenFamily") String tokenFamily);

    // Find all tokens in a family (for token rotation security)
    List<RefreshToken> findByTokenFamily(String tokenFamily);

    // Revoke all tokens for a user
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.employeeName = :employeeName")
    void revokeAllTokensForEmployee(@Param("employeeName") String employeeName);

    // Revoke all tokens in a family (when one is compromised) - now returns count
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.tokenFamily = :tokenFamily AND rt.revoked = false")
    int revokeTokenFamily(@Param("tokenFamily") String tokenFamily);

    // Clean up expired tokens (scheduled job)
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expirationDate < :now OR rt.revoked = true")
    void deleteExpiredAndRevokedTokens(@Param("now") LocalDateTime now);

    // Count active sessions for a user
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.employeeName = :employeeName AND rt.revoked = false AND rt.expirationDate > :now")
    long countActiveSessionsForEmployee(@Param("employeeName") String employeeName, @Param("now") LocalDateTime now);
}