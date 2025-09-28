package com.example.NIMASA.NYSC.Clearance.Form.controller;

import com.example.NIMASA.NYSC.Clearance.Form.DTOs.*;
import com.example.NIMASA.NYSC.Clearance.Form.Enums.UserRole;
import com.example.NIMASA.NYSC.Clearance.Form.FormStatus;
import com.example.NIMASA.NYSC.Clearance.Form.SecurityService.UnifiedAuthService;
import com.example.NIMASA.NYSC.Clearance.Form.SecurityService.RateLimitService;
import com.example.NIMASA.NYSC.Clearance.Form.model.ClearanceForm;
import com.example.NIMASA.NYSC.Clearance.Form.model.CorpsMember;
import com.example.NIMASA.NYSC.Clearance.Form.model.Employee;
import com.example.NIMASA.NYSC.Clearance.Form.repository.ClearanceRepository;
import com.example.NIMASA.NYSC.Clearance.Form.repository.CorpsMemberRepository;
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
import java.util.UUID;

/**
 * UNIFIED AUTH CONTROLLER
 * -------------------------------------------------------------
 * This is the "entry point" for authentication and employee
 * management endpoints.
 *
 * Responsibilities:
 *   - Login / Refresh / Logout
 *   - Get current user info
 *   - Admin-only employee management (add, editing, deactivate, stats)
 *   - Rate limit monitoring
 *
 * Think of this as the "reception desk" that routes requests
 * to the UnifiedAuthService (the brain).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/unified-auth")
@Validated
@Tag(name = "Enhanced Authentication", description = "Secure authentication with dual tokens and rate limiting")
public class UnifiedAuthController {

    private final UnifiedAuthService unifiedAuthService;
    private final RateLimitService rateLimitService;
    private final EmployeeRepository employeeRepository;
    private final CorpsMemberRepository corpsMemberRepository;
    private final ClearanceRepository clearanceRepository;

    // ============================================================
    // AUTHENTICATION ENDPOINTS
    // ============================================================

    @PostMapping("/login")
    @Operation(summary = "Secure login with dual token system")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequestDTO request,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse response) {
        try {
            AuthResponseDTO authResponse = unifiedAuthService.authenticate(request, httpRequest, response);
            return ResponseEntity.ok(authResponse);
        } catch (RuntimeException e) {
            // Include rate limit info in error response
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
            return ResponseEntity.status(401).body(Map.of(
                    "error", e.getMessage(),
                    "action", "login_required"
            ));
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

            return ResponseEntity.ok(Map.of(
                    "message", message,
                    "loggedOutFromAllDevices", logoutAllDevices
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Logout failed: " + e.getMessage()
            ));
        }
    }

    // ============================================================
    // USER INFO ENDPOINTS
    // ============================================================

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user details")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of(
                        "error", "Not authenticated",
                        "action", "login_required"
                ));
            }

            String username = authentication.getName();
            CurrentUserResponseDTO currentUser = unifiedAuthService.getCurrentUser(request, username);

            return ResponseEntity.ok(currentUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/session/info")
    @Operation(summary = "Get current session information")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> getSessionInfo(HttpServletRequest request) {
        try {
            return ResponseEntity.ok(Map.of(
                    "message", "Session active",
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Session expired"));
        }
    }

    // ============================================================
    // ADMIN → EMPLOYEE MANAGEMENT
    // ============================================================

    @GetMapping("/admin/employees/employeeList")
    @Operation(summary = "Get list of supervisors and HODs for admin management")
    @SecurityRequirement(name= "Bearer Authentication")
    public ResponseEntity<?> getEmployeeList() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body("Authentication is required");
            }

            EmployeePrincipal principal = (EmployeePrincipal) authentication.getPrincipal();
            if (principal.getEmployee().getRole() != UserRole.ADMIN) {
                return ResponseEntity.status(403).body("Access denied. Only Admin roles can access");
            }

            List<EmployeeListResponseDTO> employeeList = unifiedAuthService.getEmployeeList();

            Map<String, Object> response = new HashMap<>();
            response.put("employees", employeeList);
            response.put("totalCount", employeeList.size());
            response.put("supervisorCount", employeeList.stream().filter(emp -> emp.getUserRole() == UserRole.SUPERVISOR).count());
            response.put("hodCount", employeeList.stream().filter(emp -> emp.getUserRole() == UserRole.HOD).count());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
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
            employee.setPassword(null); // Hide password

            return ResponseEntity.ok(Map.of(
                    "message", "Employee added successfully",
                    "employee", Map.of(
                            "id", employee.getId(),
                            "username", employee.getUsername(),   //  auto-generated username
                            "name", employee.getName(),           // full name (for display in UI)
                            "department", employee.getDepartment(),
                            "role", employee.getRole(),
                            "active", employee.isActive()
                    )
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to add employee: " + e.getMessage()
            ));
        }
    }
    @PatchMapping("/employee/{id}/edit")
    @Operation(summary = "Edit employee  details(password, department, role")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> editEmployee(@PathVariable UUID id, @RequestBody EditEmployeeDTO dto){
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body("Authentication required");
            }

            EmployeePrincipal principal = (EmployeePrincipal) authentication.getPrincipal();
            if (principal.getEmployee().getRole() != UserRole.ADMIN) {
                return ResponseEntity.status(403).body("Access denied. Admin role required.");
            }
            Employee updated= unifiedAuthService.editEmployee(id,dto);
            updated.setPassword(null);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Employee updated successfully");
            response.put("employee", updated);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Editing Failed: " + e.getMessage());
        }
    }


    @PostMapping("/employee/{id}/deactivate")
    @Operation(summary = "Deactivate employee(Admin only)")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> deactivateEmployee(@PathVariable UUID id,
                                                @RequestBody DeactivateEmployeeDTO dto) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body("Authentication required");
            }

            EmployeePrincipal principal = (EmployeePrincipal) authentication.getPrincipal();
            if (principal.getEmployee().getRole() != UserRole.ADMIN) {
                return ResponseEntity.status(403).body("Access denied. Admin role required.");
            }
            String deactivated = unifiedAuthService.deactivateEmployee(id,principal.getEmployee().getName(),dto.getReason());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Employee deactivated successfully");
            response.put("employee", deactivated);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Deactivation Failes: " + e.getMessage());
        }

    }


    @PostMapping("/bootstrap/initial-admin")
    @Operation(summary = "Create initial admin - only works if no employees exist")
    public ResponseEntity<?> createInitialAdmin() {
        try {
            Employee admin = unifiedAuthService.createInitialAdmin();
            admin.setPassword(null); // Hide password

            return ResponseEntity.ok(Map.of(
                    "message", "Initial admin created successfully",
                    "admin", admin,
                    "defaultPassword", "admin123",
                    "warning", "Please change the default password immediately!"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to create initial admin: " + e.getMessage()
            ));
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
            List<CorpsMember> allCorpsMembers = corpsMemberRepository.findAll();
            List<ClearanceForm> allForms = clearanceRepository.findAll();
            Map<String, Object> stats = new HashMap<>();


            stats.put("totalCorpsMembers", allCorpsMembers.size());
            stats.put("totalEmployees", allEmployees.size());
            stats.put("activeEmployees", allEmployees.stream().filter(Employee::isActive).count());
            stats.put("inactiveEmployees", allEmployees.stream().filter(emp -> !emp.isActive()).count());
            stats.put("supervisors", allEmployees.stream().filter(emp -> emp.getRole() == UserRole.SUPERVISOR).count());
            stats.put("hods", allEmployees.stream().filter(emp -> emp.getRole() == UserRole.HOD).count());
            stats.put("admins", allEmployees.stream().filter(emp -> emp.getRole() == UserRole.ADMIN).count());
            stats.put("employeesWithExpiredPasswords", allEmployees.stream()
                    .filter(emp -> emp.getLastPasswordChange().isBefore(LocalDate.now().minusMonths(3)))
                    .count());


            stats.put("totalForms", allForms.size());
            stats.put("pendingForms", allForms.stream()
                    .filter(form ->
                                form.getStatus() == FormStatus.PENDING_SUPERVISOR
                            || form.getStatus() == FormStatus.PENDING_HOD
                            || form.getStatus() == FormStatus.PENDING_ADMIN)
                    .count());
            stats.put("approvedForms", allForms.stream()
                    .filter(form -> form.getStatus() == FormStatus.APPROVED)
                    .count());
            stats.put("rejectedForms", allForms.stream()
                    .filter(form -> form.getStatus() == FormStatus.REJECTED)
                    .count());

            return ResponseEntity.ok(stats);


        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to retrieve stats: " + e.getMessage()
            ));
        }
    }

    // ============================================================
    // RATE LIMIT INFO
    // ============================================================

    @PostMapping("/rate-limit/status")
    @Operation(summary = "Check rate limit status for current IP")
    public ResponseEntity<?> checkRateLimit(HttpServletRequest request) {
        String clientIp = getClientIp(request);

        Map<String, Object> rateLimitStatus = new HashMap<>();
        rateLimitStatus.put("clientIp", clientIp);
        rateLimitStatus.put("remainingAttempts", rateLimitService.getRemainingAttempts(clientIp));
        rateLimitStatus.put("isAllowed", rateLimitService.isLoginAllowed(clientIp));

        if (!rateLimitService.isLoginAllowed(clientIp)) {
            rateLimitStatus.put("retryAfterMinutes", rateLimitService.getTimeUntilNextAttemptMinutes(clientIp));
        }

        return ResponseEntity.ok(rateLimitStatus);
    }

    // ============================================================
    // UTILITY
    // ============================================================

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // ===================================================
    // CORPS MEMBERS ENDPOINTS
    // ===================================================
    @GetMapping("/admin/corps-members/list")
    @Operation(summary = "Get list of all corps members (Admin only)")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> getCorpsMemberList() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Authentication required");
        }

        EmployeePrincipal principal = (EmployeePrincipal) authentication.getPrincipal();
        if (principal.getEmployee().getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(403).body("Access denied. Only Admin can access");
        }

        List<CorpsMembersListResponseDTO> list = unifiedAuthService.getCorpsMemberList();
        return ResponseEntity.ok(Map.of(
                "corpsMembers", list,
                "totalCount", list.size()
        ));
    }

    @DeleteMapping("/admin/corps-members/{id}/deactivate")
    @Operation(summary = "Deactivate (delete) corps member (Admin only)")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> deactivateCorpsMember(@PathVariable UUID id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Authentication required");
        }

        EmployeePrincipal principal = (EmployeePrincipal) authentication.getPrincipal();
        if (principal.getEmployee().getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(403).body("Access denied. Only Admin can access");
        }

        String message = unifiedAuthService.deactivateCorpsMember(id);
        return ResponseEntity.ok(Map.of("message", message));
    }

}

