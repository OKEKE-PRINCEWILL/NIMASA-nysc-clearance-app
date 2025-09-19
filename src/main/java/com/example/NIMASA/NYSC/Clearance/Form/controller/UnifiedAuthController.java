
package com.example.NIMASA.NYSC.Clearance.Form.controller;

import com.example.NIMASA.NYSC.Clearance.Form.DTOs.*;
import com.example.NIMASA.NYSC.Clearance.Form.Enums.UserRole;
import com.example.NIMASA.NYSC.Clearance.Form.SecurityService.UnifiedAuthService;
import com.example.NIMASA.NYSC.Clearance.Form.SecurityService.RateLimitService;
import com.example.NIMASA.NYSC.Clearance.Form.model.Employee;
import com.example.NIMASA.NYSC.Clearance.Form.repository.EmployeeRepository;
import com.example.NIMASA.NYSC.Clearance.Form.securityModel.EmployeePrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/unified-auth")
@Validated
@Tag(name = "Enhanced Authentication", description = "Secure authentication with dual tokens and rate limiting")
public class UnifiedAuthController {

    private final UnifiedAuthService unifiedAuthService;
    private final RateLimitService rateLimitService;
    private final EmployeeRepository employeeRepository;

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
    @GetMapping("/admin/employees/employeeList")
    @Operation(summary = "Get list of supervisors and HODs for admin management",
            description = "Returns a list of employees with SUPERVISOR and HOD roles that can be managed by admin")


    @SecurityRequirement(name= "Bearer Authentication")
    public ResponseEntity<?> getEmployeeList(){
        try{
            Authentication authentication= SecurityContextHolder.getContext().getAuthentication();
            if(authentication== null || !authentication.isAuthenticated()){
                return ResponseEntity.status(401).body("Authentication is required");
            }

            EmployeePrincipal principal= (EmployeePrincipal) authentication.getPrincipal();
            if(principal.getEmployee().getRole()!= UserRole.ADMIN){
                return ResponseEntity.status(403).body("Access denied. Only Admin roles can access");
            }
            List<EmployeeListResponseDTO> employeeList = unifiedAuthService.getEmployeeList();

            Map<String, Object> response = new HashMap<>();
            response.put("employees", employeeList);
            response.put("totalCount", employeeList.size());
            response.put("supervisorCount", employeeList.stream()
                    .mapToLong(emp -> emp.getUserRole() == UserRole.SUPERVISOR ? 1 : 0).sum());
            response.put("hodCount", employeeList.stream()
                    .mapToLong(emp -> emp.getUserRole() == UserRole.HOD ? 1 : 0).sum());

            return ResponseEntity.ok(response);
        }
        catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Failed to retrieve employees: " + e.getMessage());
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
    @PostMapping("/employee/{name}/deactivate")
    @Operation(
            summary = "Deactivate employee by name (Admin only)",
            description = "Allows an authenticated admin to deactivate a supervisor or HOD employee. The admin's name is logged as the deactivator."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> deactivateEmployeeByName(
            @PathVariable String name,
            @Valid @RequestBody DeactivateEmployeeDTO dto){
        try {
            // Get authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body("Authentication required");
            }
            EmployeePrincipal principal = (EmployeePrincipal) authentication.getPrincipal();
            if (principal.getEmployee().getRole() != UserRole.ADMIN) {
                return ResponseEntity.status(403).body("Access denied. Admin role required.");
            }
            String adminName = principal.getEmployee().getName();

            if (adminName.equals(name)){
                return ResponseEntity.badRequest().body("Cannot deactivate an admin or your own account");
            }
            Employee deactivatedEmployeeByName= unifiedAuthService.deactivateEmployeeByName(name, adminName, dto.getReason());
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Employee deactivated successfully");
            response.put("employeeName", name);
            //response.put("employeeName", deactivatedEmployee.getName());
            response.put("employeeRole", deactivatedEmployeeByName.getRole());
            response.put("employeeDepartment", deactivatedEmployeeByName.getDepartment());
            response.put("deactivatedBy", adminName);
            response.put("reason", dto.getReason());
            response.put("timestamp", LocalDate.now());

            return ResponseEntity.ok(response);
        }
        catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Deactivation failed: " + e.getMessage());
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

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user details")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        try {
            // Get the authenticated user from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Not authenticated");
                errorResponse.put("action", "login_required");
                return ResponseEntity.status(401).body(errorResponse);
            }

            String username = authentication.getName();
            CurrentUserResponseDTO currentUser = unifiedAuthService.getCurrentUser(request, username);

            return ResponseEntity.ok(currentUser);

        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/admin/employees/stats")
    @Operation(summary = "Get employee statistics for admin dashboard")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> getEmployeeStats() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }
            EmployeePrincipal principal = (EmployeePrincipal) authentication.getPrincipal();
            if (principal.getEmployee().getRole() != UserRole.ADMIN) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied. Admin role required."));
            }
            List<Employee> allEmployees = employeeRepository.findAll();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalEmployees", allEmployees.size());
            stats.put("activeEmployees", allEmployees.stream().mapToLong(emp -> emp.isActive() ? 1 : 0).sum());
            stats.put("inactiveEmployees", allEmployees.stream().mapToLong(emp -> !emp.isActive() ? 1 : 0).sum());
            stats.put("supervisors", allEmployees.stream().mapToLong(emp -> emp.getRole() == UserRole.SUPERVISOR ? 1 : 0).sum());
            stats.put("hods", allEmployees.stream().mapToLong(emp -> emp.getRole() == UserRole.HOD ? 1 : 0).sum());
            stats.put("admins", allEmployees.stream().mapToLong(emp -> emp.getRole() == UserRole.ADMIN ? 1 : 0).sum());

            // Count employees with expired passwords
            long expiredPasswords = allEmployees.stream()
                    .mapToLong(emp -> emp.getLastPasswordChange().isBefore(LocalDate.now().minusMonths(3)) ? 1 : 0)
                    .sum();
            stats.put("employeesWithExpiredPasswords", expiredPasswords);

            return ResponseEntity.ok(stats);

        }
        catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve stats: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}