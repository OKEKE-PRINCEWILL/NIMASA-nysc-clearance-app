
package com.example.NIMASA.NYSC.Clearance.Form.SecurityService;

import com.example.NIMASA.NYSC.Clearance.Form.DTOs.CurrentUserResponseDTO;
import com.example.NIMASA.NYSC.Clearance.Form.model.CorpsMember;
import com.example.NIMASA.NYSC.Clearance.Form.DTOs.AuthRequestDTO;
import com.example.NIMASA.NYSC.Clearance.Form.DTOs.AuthResponseDTO;
import com.example.NIMASA.NYSC.Clearance.Form.DTOs.RefreshTokenResponseDTO;
import com.example.NIMASA.NYSC.Clearance.Form.model.Employee;
import com.example.NIMASA.NYSC.Clearance.Form.Enums.UserRole;
import com.example.NIMASA.NYSC.Clearance.Form.model.RefreshToken;
import com.example.NIMASA.NYSC.Clearance.Form.repository.CorpsMemberRepository;
import com.example.NIMASA.NYSC.Clearance.Form.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UnifiedAuthService {

    private final EmployeeRepository employeeRepository;
    private final CorpsMemberRepository corpsMemberRepository;
    private final BCryptPasswordEncoder encoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RateLimitService rateLimitService;

    @Value("${security.cookie.secure:true}")
    private boolean secureCookies;

    @Value("${security.cookie.same-site:None}")
    private String sameSite;

    public AuthResponseDTO authenticate(AuthRequestDTO request, HttpServletRequest httpRequest, HttpServletResponse response) {
        String clientIp = getClientIp(httpRequest);

        // Rate limiting check
        if (!rateLimitService.isLoginAllowed(clientIp)) {
            throw new RuntimeException("Too many login attempts. Please try again later.");
        }

        try {
            Optional<Employee> employeeOpt = employeeRepository.findByNameAndActive(request.getName(), true);

            if (employeeOpt.isPresent()) {
                Employee employee = employeeOpt.get();

                // Check if password was provided
                if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                    return createPasswordRequiredResponse(employee);
                }

                // Validate password
                if (!encoder.matches(request.getPassword(), employee.getPassword())) {
                    rateLimitService.recordFailedLogin(clientIp);
                    throw new RuntimeException("Invalid password. Please provide the correct password.");
                }

                // Check password expiration
                if (employee.getLastPasswordChange().isBefore(LocalDate.now().minusMonths(3))) {
                    throw new RuntimeException("Password has expired. Please change your password.");
                }

                // Successful login - reset rate limit
                rateLimitService.recordSuccessfulLogin(clientIp);

                return createEmployeeSuccessResponse(employee, httpRequest, response);
            } else {
                return handleCorpsMember(request);
            }
        } catch (RuntimeException e) {
            rateLimitService.recordFailedLogin(clientIp);
            throw e;
        }
    }

    /**
     * Refresh access token using refresh token from cookie
     */
    public RefreshTokenResponseDTO refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
        // Extract refresh token from cookie
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken == null) {
            throw new RuntimeException("Refresh token not found");
        }

        // Validate refresh token
        Optional<String> employeeNameOpt = refreshTokenService.validateRefreshToken(refreshToken);
        if (employeeNameOpt.isEmpty()) {
            throw new RuntimeException("Invalid refresh token");
        }

        String employeeName = employeeNameOpt.get();

        // Get employee details
        Employee employee = employeeRepository.findByNameAndActive(employeeName, true)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // Rotate refresh token (security best practice)
        String deviceInfo = extractDeviceInfo(request);
        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(refreshToken, deviceInfo);

        // Generate new access token
        String newAccessToken = jwtService.generateAccessToken(employeeName);

        // Set both tokens as cookies
        setAccessTokenCookie(response, newAccessToken);
        setRefreshTokenCookie(response, generateTokenForCookie(newRefreshToken));

        return new RefreshTokenResponseDTO(
                "Token refreshed successfully",
                jwtService.getAccessTokenExpirationMs(),
                employeeName,
                employee.getRole()
        );
    }

    /**
     * Logout user - revoke refresh tokens and clear cookies
     */
    public String logout(HttpServletRequest request, HttpServletResponse response, boolean logoutAllDevices) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        int sessionsTerminated = 0;

        if (refreshToken != null) {
            Optional<String> employeeNameOpt = refreshTokenService.validateRefreshToken(refreshToken);
            if (employeeNameOpt.isPresent()) {
                String employeeName = employeeNameOpt.get();

                if (logoutAllDevices) {
                    // Logout from all devices
                    sessionsTerminated = refreshTokenService.revokeAllTokensForEmployee(employeeName);
                } else {
                    // Just revoke current refresh token family
                    RefreshToken currentToken = findCurrentRefreshToken(refreshToken);
                    if (currentToken != null) {
                        refreshTokenService.revokeTokenFamily(currentToken.getTokenFamily());
                        sessionsTerminated = 1;
                    }
                }
            }
        }

        // Clear cookies
        clearAuthCookies(response);

        return String.format("Logged out successfully. %d session(s) terminated.", sessionsTerminated);
    }

    private AuthResponseDTO createEmployeeSuccessResponse(Employee employee, HttpServletRequest request, HttpServletResponse response) {
        // Generate token family for this login session
        String tokenFamily = jwtService.generateTokenFamily();
        String deviceInfo = extractDeviceInfo(request);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(employee.getName());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(employee.getName(), tokenFamily, deviceInfo);

        // Set secure cookies (NO tokens in response body!)
        setAccessTokenCookie(response, accessToken);
        setRefreshTokenCookie(response, generateTokenForCookie(refreshToken));

        AuthResponseDTO authResponse = new AuthResponseDTO();
        authResponse.setMessage("Employee authentication successful");
        authResponse.setName(employee.getName());
        authResponse.setDepartment(employee.getDepartment());
        authResponse.setRole(employee.getRole());
        authResponse.setUserType("EMPLOYEE");
        authResponse.setPasswordRequired(false);
        authResponse.setNewCorpsMember(false);
        authResponse.setAccessTokenExpirationMs(jwtService.getAccessTokenExpirationMs());
        authResponse.setRequiresRefresh(false);

        return authResponse;
    }

    private void setAccessTokenCookie(HttpServletResponse response, String accessToken) {
        Cookie accessCookie = new Cookie("accessToken", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false);//secureCookies);
        accessCookie.setPath("/");//"/api");
        accessCookie.setMaxAge((int) (jwtService.getAccessTokenExpirationMs() / 1000));

        // Set SameSite attribute
//        String cookieHeader = String.format("%s=%s; Path=%s; HttpOnly; SameSite=None; Secure%s; Max-Age=%d",
//                accessCookie.getName(),
//                accessCookie.getValue(),
//                accessCookie.getPath(),
//                sameSite,
//                secureCookies ? "; Secure" : "",
//                accessCookie.getMaxAge());
//
//        response.addHeader("Set-Cookie", cookieHeader);
//    }
        response.addHeader("Set-Cookie", String.format(
                "%s=%s; Path=%s; HttpOnly; SameSite=Lax; Max-Age=%d",
                accessCookie.getName(),
                accessCookie.getValue(),
                "/",
//                accessCookie.getPath(),
                accessCookie.getMaxAge()
        ));
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/api/unified-auth");  // Only for auth endpoints
        refreshCookie.setMaxAge((int) (jwtService.getRefreshTokenExpirationMs() / 1000));

        // Set SameSite attribute
        response.addHeader("Set-Cookie", String.format(
                "%s=%s; Path=%s; HttpOnly; SameSite=Lax; Max-Age=%d",
                refreshCookie.getName(),
                refreshCookie.getValue(),
                refreshCookie.getPath(),
                refreshCookie.getMaxAge()
        ));
    }

    private void clearAuthCookies(HttpServletResponse response) {
        // Clear access token cookie
        Cookie accessCookie = new Cookie("accessToken", "");
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(secureCookies);
        accessCookie.setPath("/api");
        accessCookie.setMaxAge(0);

        // Clear refresh token cookie
        Cookie refreshCookie = new Cookie("refreshToken", "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(secureCookies);
        refreshCookie.setPath("/api/unified-auth");
        refreshCookie.setMaxAge(0);

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if ("refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String extractDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String ip = getClientIp(request);
        return String.format("%s from %s", userAgent != null ? userAgent.substring(0, Math.min(100, userAgent.length())) : "Unknown", ip);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String generateTokenForCookie(RefreshToken refreshToken) {
        // This generates a JWT refresh token that corresponds to the database entry
        return jwtService.generateRefreshToken(refreshToken.getEmployeeName(), refreshToken.getTokenFamily());
    }

    private RefreshToken findCurrentRefreshToken(String rawToken) {
        // Implementation to find current refresh token (simplified)
        return null; // Would implement proper lookup
    }

    // Keep existing methods unchanged
    private AuthResponseDTO createPasswordRequiredResponse(Employee employee) {
        AuthResponseDTO response = new AuthResponseDTO();
        response.setMessage("Password required for employee authentication");
        response.setName(employee.getName());
        response.setDepartment(employee.getDepartment());
        response.setRole(employee.getRole());
        response.setUserType("EMPLOYEE");
        response.setPasswordRequired(true);
        response.setNewCorpsMember(false);
        return response;
    }

    private AuthResponseDTO handleCorpsMember(AuthRequestDTO request) {
        if (request.getRole() != UserRole.CORPS_MEMBER) {
            throw new RuntimeException("Access denied. Only employees can have " + request.getRole() + " role.");
        }

        Optional<CorpsMember> existingCorpsMember = corpsMemberRepository.findByNameAndActive(request.getName(), true);

        if (existingCorpsMember.isPresent()) {
            CorpsMember corpsMember = existingCorpsMember.get();
            return createCorpsMemberResponse(corpsMember, false);
        } else {
            CorpsMember newCorpsMember = new CorpsMember();
            newCorpsMember.setName(request.getName());
            newCorpsMember.setDepartment(request.getDepartment());
            newCorpsMember.setActive(true);
            newCorpsMember.setCreatedAt(LocalDate.now());

            CorpsMember savedCorpsMember = corpsMemberRepository.save(newCorpsMember);
            return createCorpsMemberResponse(savedCorpsMember, true);
        }
    }

    private AuthResponseDTO createCorpsMemberResponse(CorpsMember corpsMember, boolean isNew) {
        AuthResponseDTO response = new AuthResponseDTO();
        response.setMessage(isNew ? "New corps member registered successfully" : "Corps member authentication successful");
        response.setName(corpsMember.getName());
        response.setDepartment(corpsMember.getDepartment());
        response.setRole(UserRole.CORPS_MEMBER);
        response.setUserType("CORPS_MEMBER");
        response.setPasswordRequired(false);
        response.setNewCorpsMember(isNew);
        return response;
    }

    // Existing employee management methods remain unchanged
    public Employee addEmployee(String name, String password, String department, UserRole role) {
        if (employeeRepository.existsByName(name)) {
            throw new RuntimeException("Employee with this name already exists");
        }

        if (role != UserRole.ADMIN && role != UserRole.SUPERVISOR && role != UserRole.HOD) {
            throw new RuntimeException("Invalid role for employee. Only ADMIN, SUPERVISOR, HOD are allowed.");
        }

        Employee employee = new Employee();
        employee.setName(name);
        employee.setPassword(encoder.encode(password));
        employee.setDepartment(department);
        employee.setRole(role);
        employee.setActive(true);
        employee.setCreatedAt(LocalDate.now());
        employee.setLastPasswordChange(LocalDate.now());

        return employeeRepository.save(employee);
    }

    public void changeEmployeePassword(String name, String newPassword) {
        Employee employee = employeeRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        employee.setPassword(encoder.encode(newPassword));
        employee.setLastPasswordChange(LocalDate.now());
        employeeRepository.save(employee);
    }

    public void deactivateEmployee(String name) {
        Employee employee = employeeRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        employee.setActive(false);
        employeeRepository.save(employee);
    }

    public Employee createInitialAdmin() {
        if (employeeRepository.count() > 0) {
            throw new RuntimeException("Employees already exist. Use normal add employee endpoint.");
        }

        Employee admin = new Employee();
        admin.setName("Initial Admin");
        admin.setPassword(encoder.encode("admin123"));
        admin.setDepartment("Administration");
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);
        admin.setCreatedAt(LocalDate.now());
        admin.setLastPasswordChange(LocalDate.now());

        return employeeRepository.save(admin);
    }
    /**
     * Get current authenticated user details
     */
    public CurrentUserResponseDTO getCurrentUser(HttpServletRequest request, String username) {
        // Find the user (employee or corps member)
        Optional<Employee> employeeOpt = employeeRepository.findByNameAndActive(username, true);

        if (employeeOpt.isPresent()) {
            Employee employee = employeeOpt.get();

            // Extract tokens from cookies
            String accessToken = extractAccessTokenFromCookie(request);

            CurrentUserResponseDTO response = new CurrentUserResponseDTO();
            response.setId(employee.getId());
            response.setName(employee.getName());
            response.setDepartment(employee.getDepartment());
            response.setRole(employee.getRole());
            response.setUserType("EMPLOYEE");
            response.setActive(employee.isActive());
            response.setCreatedAT(employee.getCreatedAt());
            response.setLastPasswordChange(employee.getLastPasswordChange());

            // Check if password is expired
            boolean passwordExpired = employee.getLastPasswordChange().isBefore(LocalDate.now().minusMonths(3));
            response.setPasswordExpired(passwordExpired);

            // Token info
            if (accessToken != null) {
                long remainingMinutes = jwtService.getTokenRemainingTimeMinutes(accessToken);
                response.setAccessTokenRemainingMinutes(remainingMinutes);
            }

            response.setRefreshTokenExpirationMs(jwtService.getRefreshTokenExpirationMs());

            // Get active session count
            long sessionCount = refreshTokenService.getActiveSessionCount(employee.getName());
            response.setActiveSessionCount(sessionCount);

            response.setAuthenticated(true);

            return response;

        } else {
            // Check if it's a corps member
            Optional<CorpsMember> corpsMemberOpt = corpsMemberRepository.findByNameAndActive(username, true);

            if (corpsMemberOpt.isPresent()) {
                CorpsMember corpsMember = corpsMemberOpt.get();

                CurrentUserResponseDTO response = new CurrentUserResponseDTO();
                response.setId(corpsMember.getId());
                response.setName(corpsMember.getName());
                response.setDepartment(corpsMember.getDepartment());
                response.setRole(UserRole.CORPS_MEMBER);
                response.setUserType("CORPS_MEMBER");
                response.setActive(corpsMember.isActive());
                response.setCreatedAT(corpsMember.getCreatedAt());
                response.setAuthenticated(true);

                // Corps members don't have sessions or password expiration
                response.setPasswordExpired(false);
                response.setActiveSessionCount(0);

                return response;
            }
        }

        throw new RuntimeException("User not found");
    }

    private String extractAccessTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if ("accessToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}