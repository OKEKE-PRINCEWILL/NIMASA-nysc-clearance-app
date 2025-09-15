
package com.example.NIMASA.NYSC.Clearance.Form.controller;

import com.example.NIMASA.NYSC.Clearance.Form.DTOs.*;
import com.example.NIMASA.NYSC.Clearance.Form.SecurityService.UnifiedAuthService;
import com.example.NIMASA.NYSC.Clearance.Form.SecurityService.RateLimitService;
import com.example.NIMASA.NYSC.Clearance.Form.model.Employee;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/unified-auth")
@Validated
@Tag(name = "Enhanced Authentication", description = "Secure authentication with dual tokens and rate limiting")
public class UnifiedAuthController {

    private final UnifiedAuthService unifiedAuthService;
    private final RateLimitService rateLimitService;

    @PostMapping("/login")
    @Operation(summary = "Secure login with dual token system")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequestDTO request,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse response) {
        try {
            AuthResponseDTO authResponse = unifiedAuthService.authenticate(request, httpRequest, response);
            return ResponseEntity.ok(authResponse);
        } catch (RuntimeException e) {
            // Include rate limiting info in error response
            String clientIp = getClientIp(httpRequest);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("remainingAttempts", rateLimitService.getRemainingAttempts(clientIp));

            if (rateLimitService.getRemainingAttempts(clientIp) == 0) {
                errorResponse.put("retryAfterMinutes", rateLimitService.getTimeUntilNextAttemptMinutes(clientIp));
            }

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using secure cookie")
    public ResponseEntity<?> refreshToken(HttpServletRequest request,
                                          HttpServletResponse response,
                                          @RequestBody(required = false) RefreshTokenRequestDTO refreshRequest) {
        try {
            RefreshTokenResponseDTO refreshResponse = unifiedAuthService.refreshAccessToken(request, response);
            return ResponseEntity.ok(refreshResponse);
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("action", "login_required");
            return ResponseEntity.status(401).body(errorResponse);
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Secure logout with session termination")
    public ResponseEntity<?> logout(HttpServletRequest request,
                                    HttpServletResponse response,
                                    @RequestBody(required = false) LogoutRequestDTO logoutRequest) {
        try {
            boolean logoutAllDevices = logoutRequest != null && logoutRequest.isLogoutAllDevices();
            String message = unifiedAuthService.logout(request, response, logoutAllDevices);

            Map<String, Object> logoutResponse = new HashMap<>();
            logoutResponse.put("message", message);
            logoutResponse.put("loggedOutFromAllDevices", logoutAllDevices);

            return ResponseEntity.ok(logoutResponse);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Logout failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/employee/add")
    @Operation(summary = "Add new employee (Admin only)")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> addEmployee(@Valid @RequestBody AddEmployeeDTO employeeDTO) {
        try {
            Employee employee = unifiedAuthService.addEmployee(
                    employeeDTO.getName(),
                    employeeDTO.getPassword(),
                    employeeDTO.getDepartment(),
                    employeeDTO.getRole()
            );

            // Don't return password
            employee.setPassword(null);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Employee added successfully");
            response.put("employee", employee);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to add employee: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/employee/change-password")
    @Operation(summary = "Change employee password")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> changeEmployeePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        try {
            unifiedAuthService.changeEmployeePassword(dto.getUsername(), dto.getNewPassword());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Password changed successfully");
            response.put("username", dto.getUsername());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Password change failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/employee/deactivate")
    @Operation(summary = "Deactivate employee (Admin only)")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> deactivateEmployee(@RequestParam String name) {
        try {
            unifiedAuthService.deactivateEmployee(name);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Employee deactivated successfully");
            response.put("employeeName", name);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to deactivate employee: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/bootstrap/initial-admin")
    @Operation(summary = "Create initial admin - only works if no employees exist")
    public ResponseEntity<?> createInitialAdmin() {
        try {
            Employee admin = unifiedAuthService.createInitialAdmin();
            admin.setPassword(null); // Don't return password

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Initial admin created successfully");
            response.put("admin", admin);
            response.put("defaultPassword", "admin123");
            response.put("warning", "Please change the default password immediately!");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to create initial admin: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/session/info")
    @Operation(summary = "Get current session information")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> getSessionInfo(HttpServletRequest request) {
        try {
            // This endpoint would provide session info for authenticated users
            Map<String, Object> sessionInfo = new HashMap<>();
            sessionInfo.put("message", "Session active");
            sessionInfo.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(sessionInfo);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Session expired"));
        }
    }

    @PostMapping("/rate-limit/status")
    @Operation(summary = "Check rate limit status for current IP")
    public ResponseEntity<?> checkRateLimit(HttpServletRequest request) {
        String clientIp = getClientIp(request);

        Map<String, Object> rateLimitStatus = new HashMap<>();
        rateLimitStatus.put("clientIp", clientIp);
        rateLimitStatus.put("remainingAttempts", rateLimitService.getRemainingAttempts(clientIp));
        rateLimitStatus.put("isAllowed", rateLimitService.isLoginAllowed(clientIp));

        if (!rateLimitStatus.get("isAllowed").equals(true)) {
            rateLimitStatus.put("retryAfterMinutes", rateLimitService.getTimeUntilNextAttemptMinutes(clientIp));
        }

        return ResponseEntity.ok(rateLimitStatus);
    }

    /**
     * Utility method to extract client IP
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}