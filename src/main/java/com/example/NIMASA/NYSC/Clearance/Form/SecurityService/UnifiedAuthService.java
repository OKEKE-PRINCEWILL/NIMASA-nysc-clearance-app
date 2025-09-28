package com.example.NIMASA.NYSC.Clearance.Form.SecurityService;

import com.example.NIMASA.NYSC.Clearance.Form.DTOs.*;
import com.example.NIMASA.NYSC.Clearance.Form.FormStatus;
import com.example.NIMASA.NYSC.Clearance.Form.Enums.UserRole;
import com.example.NIMASA.NYSC.Clearance.Form.model.CorpsMember;
import com.example.NIMASA.NYSC.Clearance.Form.model.Employee;
import com.example.NIMASA.NYSC.Clearance.Form.model.RefreshToken;
import com.example.NIMASA.NYSC.Clearance.Form.repository.ClearanceRepository;
import com.example.NIMASA.NYSC.Clearance.Form.repository.CorpsMemberRepository;
import com.example.NIMASA.NYSC.Clearance.Form.repository.EmployeeRepository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UNIFIED AUTH SERVICE
 * -------------------------------------------------------------
 * This service is the "brain" for authentication in our system.
 * It handles:
 *   - Employee login/logout (with JWT + refresh tokens)
 *   - Corps member login (simpler, no password)
 *   - Token rotation + cookie management
 *   - Employee management (add, deactivate, password change)
 *   - Quick in-memory cache to speed up repeated logins
 *
 * Think of this as the "gatekeeper" for NIMASA’s Clearance System.
 */
@Service
@RequiredArgsConstructor
public class UnifiedAuthService {

    // ========== DEPENDENCIES ==========
    private final EmployeeRepository employeeRepository;
    private final CorpsMemberRepository corpsMemberRepository;
    private final ClearanceRepository clearanceRepository;
    private final BCryptPasswordEncoder encoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RateLimitService rateLimitService;

    // Cookie rules (from application.properties / yml)
    @Value("${security.cookie.secure:true}")
    private boolean secureCookies;

    @Value("${security.cookie.same-site:None}")
    private String sameSite;

    // ========== CACHING ==========
    // We keep a 5-minute cache of recently authenticated employees
    // This avoids hitting the DB on every single login attempt.
    private final Map<String, CachedEmployeeData> quickCache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = 300000; // 5 minutes

    /**
     * Tiny class to wrap cached employee data.
     * Stores the employee + when it was cached.
     */
    private static class CachedEmployeeData {
        final Employee employee;
        final long timestamp;

        CachedEmployeeData(Employee employee) {
            this.employee = employee;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > CACHE_DURATION_MS;
        }
    }

    // ============================================================
    // AUTHENTICATION FLOW
    // ============================================================

    /**
     * Main login method.
     * 1. Checks rate limiting (blocks brute force).
     * 2. Tries to fetch employee from cache or DB.
     * 3. If not employee → checks corps member login.
     */
    public AuthResponseDTO authenticate(AuthRequestDTO request,
                                        HttpServletRequest httpRequest,
                                        HttpServletResponse response) {
        String clientIp = getClientIp(httpRequest);

        // Protect system: stop too many wrong attempts
        if (!rateLimitService.isLoginAllowed(clientIp)) {
            throw new RuntimeException("Too many login attempts. Please try again later.");
        }

        try {
            // Step 1: try cache
            String cacheKey = request.getName().toLowerCase().trim();
            CachedEmployeeData cached = quickCache.get(cacheKey);

            if (cached != null && !cached.isExpired()) {
                return authenticateFromCache(cached.employee, request, httpRequest, response, clientIp);
            }

            // Step 2: query DB for employee
            // NEW employee lookup (by username)
            CompletableFuture<Optional<Employee>> employeeQuery =
                    CompletableFuture.supplyAsync(() -> employeeRepository.findByUsernameIgnoreCaseAndActive(request.getName(), true));

            Optional<Employee> employeeOpt = employeeQuery.join();

            if (employeeOpt.isPresent()) {
                Employee employee = employeeOpt.get();
                quickCache.put(cacheKey, new CachedEmployeeData(employee));
                return authenticateFromCache(employee, request, httpRequest, response, clientIp);
            } else {
                // Step 3: fallback → corps member login
                return handleCorpsMember(request);
            }

        } catch (RuntimeException e) {
            rateLimitService.recordFailedLogin(clientIp);
            throw e;
        }
    }

    /**
     * Validate employee login (using cached or fresh DB record).
     */
    private AuthResponseDTO authenticateFromCache(Employee employee,
                                                  AuthRequestDTO request,
                                                  HttpServletRequest httpRequest,
                                                  HttpServletResponse response,
                                                  String clientIp) {
        // Password required
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return createPasswordRequiredResponse(employee);
        }

        // Check password
        if (!encoder.matches(request.getPassword(), employee.getPassword())) {
            rateLimitService.recordFailedLogin(clientIp);
            throw new RuntimeException("Invalid password. Please provide the correct password.");
        }

        // Force password change every 3 months
        if (employee.getLastPasswordChange().isBefore(LocalDate.now().minusMonths(3))) {
            throw new RuntimeException("Password has expired. Please change your password.");
        }

        // All good → reset limiter + issue tokens
        rateLimitService.recordSuccessfulLogin(clientIp);
        return createEmployeeSuccessResponse(employee, httpRequest, response);
    }

    /**
     * Corps members have no password. They are auto-registered on first login.
     */
    private AuthResponseDTO handleCorpsMember(AuthRequestDTO request) {
        if (request.getRole() != UserRole.CORPS_MEMBER) {
            throw new RuntimeException("Access denied. Only employees can have " + request.getRole() + " role.");
        }

        Optional<CorpsMember> existing = corpsMemberRepository.findByNameIgnoreCaseAndActive(request.getName(), true);

        if (existing.isPresent()) {
            return createCorpsMemberResponse(existing.get(), false);
        } else {
            CorpsMember newCorpsMember = new CorpsMember();
            newCorpsMember.setName(request.getName());
            newCorpsMember.setDepartment(request.getDepartment());
            newCorpsMember.setActive(true);
            newCorpsMember.setCreatedAt(LocalDate.now());

            CorpsMember saved = corpsMemberRepository.save(newCorpsMember);
            return createCorpsMemberResponse(saved, true);
        }
    }

    // ============================================================
    // USER INFO
    // ============================================================

    /**
     * Get details of whoever is logged in (employee or corps).
     * Useful for dashboards showing "current user".
     */
    public CurrentUserResponseDTO getCurrentUser(HttpServletRequest request, String username) {
        // First check employee
        Optional<Employee> employeeOpt = employeeRepository.findByNameAndActive(username, true);

        if (employeeOpt.isPresent()) {
            Employee employee = employeeOpt.get();
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

            // Expired password?
            response.setPasswordExpired(employee.getLastPasswordChange().isBefore(LocalDate.now().minusMonths(3)));

            // Token + session info
            if (accessToken != null) {
                long remainingMinutes = jwtService.getTokenRemainingTimeMinutes(accessToken);
                response.setAccessTokenRemainingMinutes(remainingMinutes);
            }
            response.setRefreshTokenExpirationMs(jwtService.getRefreshTokenExpirationMs());
            response.setActiveSessionCount(refreshTokenService.getActiveSessionCount(employee.getName()));
            response.setAuthenticated(true);

            return response;

        } else {
            // If not employee, check corps member
            Optional<CorpsMember> corpsOpt = corpsMemberRepository.findByNameIgnoreCaseAndActive(username, true);

            if (corpsOpt.isPresent()) {
                CorpsMember corpsMember = corpsOpt.get();

                CurrentUserResponseDTO response = new CurrentUserResponseDTO();
                response.setId(corpsMember.getId());
                response.setName(corpsMember.getName());
                response.setDepartment(corpsMember.getDepartment());
                response.setRole(UserRole.CORPS_MEMBER);
                response.setUserType("CORPS_MEMBER");
                response.setActive(corpsMember.isActive());
                response.setCreatedAT(corpsMember.getCreatedAt());
                response.setAuthenticated(true);

                response.setPasswordExpired(false); // corps never expire
                response.setActiveSessionCount(0);  // corps have no sessions

                return response;
            }
        }

        throw new RuntimeException("User not found");
    }

    // ============================================================
    // LOGOUT + REFRESH TOKENS
    // ============================================================

    /**
     * Logout → clears cookies and optionally all sessions.
     */
    public String logout(HttpServletRequest request, HttpServletResponse response, boolean logoutAllDevices) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        int sessionsTerminated = 0;

        if (refreshToken != null) {
            try {
                if (logoutAllDevices) {
                    String employeeName = jwtService.extractUsername(refreshToken);
                    sessionsTerminated = refreshTokenService.revokeAllTokensForEmployee(employeeName);
                } else {
                    sessionsTerminated = refreshTokenService.revokeSingleSession(refreshToken);
                }
            } catch (Exception e) {
                System.err.println("Error during logout: " + e.getMessage());
            }
        }

        clearAuthCookies(response);
        return String.format("Logged out successfully. %d session(s) terminated.", sessionsTerminated);
    }

    /**
     * Refresh access token using refresh token from cookie.
     * Classic JWT rotation flow.
     */
    public RefreshTokenResponseDTO refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken == null) throw new RuntimeException("Refresh token not found");

        Optional<String> employeeNameOpt = refreshTokenService.validateRefreshToken(refreshToken);
        if (employeeNameOpt.isEmpty()) throw new RuntimeException("Invalid refresh token");

        String employeeName = employeeNameOpt.get();
        Employee employee = employeeRepository.findByNameAndActive(employeeName, true)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        String deviceInfo = extractDeviceInfo(request);
        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(refreshToken, deviceInfo);
        String newAccessToken = jwtService.generateAccessToken(employeeName);

        setAccessTokenCookie(response, newAccessToken);
        setRefreshTokenCookie(response, generateTokenForCookie(newRefreshToken));

        return new RefreshTokenResponseDTO(
                "Token refreshed successfully",
                jwtService.getAccessTokenExpirationMs(),
                employeeName,
                employee.getRole()
        );
    }

    // ============================================================
    // EMPLOYEE MANAGEMENT (Add, Passwords, Deactivate, etc.)
    // ============================================================

    // ... (the rest continues, same structure, with humanized comments)

    public List<CorpsMembersListResponseDTO> getCorpsMemberList() {
        List<CorpsMember> corpsMembers = corpsMemberRepository.findAll();

        return corpsMembers.stream().map(corps -> {
            CorpsMembersListResponseDTO dto = new CorpsMembersListResponseDTO();
            dto.setId(corps.getId());
            dto.setName(corps.getName());
            dto.setDepartment(corps.getDepartment());
            dto.setActive(corps.isActive());
            dto.setCreatedAT(corps.getCreatedAt());
            return dto;
        }).toList();
    }

    public String deactivateCorpsMember(UUID corpsId) {
        CorpsMember corpsMember = corpsMemberRepository.findById(corpsId)
                .orElseThrow(() -> new RuntimeException("Corps member not found"));

        // Only delete corps member (leave their forms intact)
        corpsMemberRepository.deleteById(corpsId);

        return String.format("Corps member %s has been removed from the system", corpsMember.getName());
    }

    public List<EmployeeListResponseDTO> getEmployeeList() {
        List<Employee> employees = employeeRepository.findAll().stream()
                .filter(emp -> emp.getRole() == UserRole.SUPERVISOR || emp.getRole() == UserRole.HOD|| emp.getRole()==UserRole.ADMIN)
                .toList();

        return employees.stream().map(employee -> {
            EmployeeListResponseDTO dto = new EmployeeListResponseDTO();
            dto.setId(employee.getId());
            dto.setName(employee.getName());
            dto.setUsername(employee.getUsername());
            dto.setDepartment(employee.getDepartment());
            dto.setUserRole(employee.getRole());
            dto.setActive(employee.isActive());
            dto.setCreatedAT(employee.getCreatedAt());
            dto.setLastPasswordChange(employee.getLastPasswordChange());

            // Check if password is expired (older than 3 months)
            boolean passwordExpired = employee.getLastPasswordChange()
                    .isBefore(LocalDate.now().minusMonths(3));
            dto.setPasswordExpired(passwordExpired);

            // Get count of pending forms for this employee
            long pendingCount = getPendingFormsCount(employee);
            dto.setFormPendingReview(pendingCount);

            return dto;
        }).toList();
    }


    /**
     * Add a new employee (Admin only).
     */
    public Employee addEmployee(String name, String password, String department, UserRole role) {

        if (role != UserRole.ADMIN && role != UserRole.SUPERVISOR && role != UserRole.HOD) {
            throw new RuntimeException("Invalid role for employee. Only ADMIN, SUPERVISOR, HOD are allowed.");
        }
        String username= generateUsername(name);
        String baseUsername= username;
        int counter=1;
        while(employeeRepository.existsByUsername(username)){
            username= baseUsername+counter;
            counter++;
        }

        Employee employee = new Employee();
        employee.setUsername(username);
        employee.setName(name);
        employee.setPassword(encoder.encode(password));
        employee.setDepartment(department);
        employee.setRole(role);
        employee.setActive(true);
        employee.setCreatedAt(LocalDate.now());
        employee.setLastPasswordChange(LocalDate.now());

        return employeeRepository.save(employee);
    }
    private String generateUsername(String fullName) {
        String[] parts = fullName.trim().toLowerCase().split(" ");
        if (parts.length < 2) {
            throw new RuntimeException("Full name must include first and last name.");
        }
        return parts[0] + "." + parts[1]; // firstname.lastname
    }


    public Employee editEmployee(UUID employeeId, EditEmployeeDTO dto) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(()-> new RuntimeException("Employee not found"));

        if (dto.getDepartment() != null && !dto.getDepartment().isBlank()){
            employee.setDepartment(dto.getDepartment());
        }

        if(dto.getRole() != null){
            employee.setRole((dto).getRole());
        }

        if(dto.getPassword()!= null && !dto.getPassword().isBlank()){
            employee.setPassword(encoder.encode(dto.getPassword()));
            employee.setLastPasswordChange(LocalDate.now());
        }

        return employeeRepository.save(employee);
    }


    public String deactivateEmployee(UUID employeeId, String adminName, String reason){
        Employee employee= employeeRepository.findById(employeeId)
                .orElseThrow(()-> new RuntimeException("Employee not found"));

//        if (employee.getRole()== UserRole.ADMIN){
//            throw new RuntimeException("Cannot deactivate admin user");
//        }

        if(employee.getName().equals(adminName)){
            throw new RuntimeException("Cannot deactivate your own account");
        }



        employeeRepository.deleteById(employeeId);

        quickCache.remove(employee.getName().toLowerCase().trim());
        refreshTokenService.revokeAllTokensForEmployee(employee.getName());

        return String.format("Employee member %s has been removed from the system", employee.getName());
    }
    /**
     * First-time system setup → create initial Admin.
     */
    public Employee createInitialAdmin() {
        if (employeeRepository.count() > 0) {
            throw new RuntimeException("Employees already exist. Use normal add employee endpoint.");
        }

        Employee admin = new Employee();
        admin.setName("Initial Admin");  // full name for UI
        admin.setUsername("Initial.Admin");      // 👈 set username for login
        admin.setPassword(encoder.encode("admin123"));
        admin.setDepartment("Administration");
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);
        admin.setCreatedAt(LocalDate.now());
        admin.setLastPasswordChange(LocalDate.now());

        return employeeRepository.save(admin);
    }


    private long getPendingFormsCount(Employee employee) {
        return switch (employee.getRole()) {
            case SUPERVISOR -> clearanceRepository.countByStatusAndDepartment(FormStatus.PENDING_SUPERVISOR, employee.getDepartment());
            case HOD -> clearanceRepository.countByStatusAndDepartment(FormStatus.PENDING_HOD, employee.getDepartment());
            default -> 0;
        };
    }

    // ============================================================
    // COOKIE + TOKEN UTILITIES
    // ============================================================

    private void setAccessTokenCookie(HttpServletResponse response, String accessToken) {
        boolean isProduction = !isLocalhost();
        String sameSite = isProduction ? "None" : "Lax";
        String secure = isProduction ? "; Secure" : "";

        response.addHeader("Set-Cookie", String.format(
                "accessToken=%s; Path=/; HttpOnly; SameSite=%s%s; Max-Age=%d",
                accessToken, sameSite, secure, (int) (jwtService.getAccessTokenExpirationMs() / 1000)
        ));
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        boolean isProduction = !isLocalhost();
        String sameSite = isProduction ? "None" : "Lax";
        String secure = isProduction ? "; Secure" : "";

        response.addHeader("Set-Cookie", String.format(
                "refreshToken=%s; Path=/api/unified-auth; HttpOnly; SameSite=%s%s; Max-Age=%d",
                refreshToken, sameSite, secure, (int) (jwtService.getRefreshTokenExpirationMs() / 1000)
        ));
    }

    private boolean isLocalhost() {
        String env = System.getenv("ENVIRONMENT");
        return env != null && env.equalsIgnoreCase("development");
    }

    private void clearAuthCookies(HttpServletResponse response) {
        Cookie accessCookie = new Cookie("accessToken", "");
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(secureCookies);
        accessCookie.setPath("/api");
        accessCookie.setMaxAge(0);

        Cookie refreshCookie = new Cookie("refreshToken", "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(secureCookies);
        refreshCookie.setPath("/api/unified-auth");
        refreshCookie.setMaxAge(0);

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
    }

    private String generateTokenForCookie(RefreshToken refreshToken) {
        return jwtService.generateRefreshToken(refreshToken.getEmployeeName(), refreshToken.getTokenFamily());
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

    // ============================================================
    // RESPONSE HELPERS
    // ============================================================

    private AuthResponseDTO createEmployeeSuccessResponse(Employee employee,
                                                          HttpServletRequest request,
                                                          HttpServletResponse response) {
        String tokenFamily = jwtService.generateTokenFamily();
        String deviceInfo = extractDeviceInfo(request);

        CompletableFuture<String> accessTokenFuture =
                CompletableFuture.supplyAsync(() -> jwtService.generateAccessToken(employee.getName()));

        CompletableFuture<RefreshToken> refreshTokenFuture =
                CompletableFuture.supplyAsync(() -> refreshTokenService.createRefreshToken(employee.getName(), tokenFamily, deviceInfo));

        String accessToken = accessTokenFuture.join();
        RefreshToken refreshToken = refreshTokenFuture.join();

        setAccessTokenCookie(response, accessToken);
        setRefreshTokenCookie(response, generateTokenForCookie(refreshToken));

        AuthResponseDTO authResponse = new AuthResponseDTO();
        authResponse.setId(employee.getId());
//        authResponse.setUsername(employee.getUsername());
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

    private AuthResponseDTO createPasswordRequiredResponse(Employee employee) {
        AuthResponseDTO response = new AuthResponseDTO();
        response.setMessage("Password required for employee authentication");
        response.setId(employee.getId());
        response.setName(employee.getName());
        response.setDepartment(employee.getDepartment());
        response.setRole(employee.getRole());
        response.setUserType("EMPLOYEE");
        response.setPasswordRequired(true);
        response.setNewCorpsMember(false);
        return response;
    }

    private AuthResponseDTO createCorpsMemberResponse(CorpsMember corpsMember, boolean isNew) {
        AuthResponseDTO response = new AuthResponseDTO();
        response.setMessage(isNew ? "New corps member registered successfully"
                : "Corps member authentication successful");
        response.setId(corpsMember.getId());
        response.setName(corpsMember.getName());
        response.setDepartment(corpsMember.getDepartment());
        response.setRole(UserRole.CORPS_MEMBER);
        response.setUserType("CORPS_MEMBER");
        response.setPasswordRequired(false);
        response.setNewCorpsMember(isNew);
        return response;
    }
}
