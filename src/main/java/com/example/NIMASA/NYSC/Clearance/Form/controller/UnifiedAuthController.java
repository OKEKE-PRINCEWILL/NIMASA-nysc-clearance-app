//package com.example.NIMASA.NYSC.Clearance.Form.controller;
//
//import com.example.NIMASA.NYSC.Clearance.Form.DTOs.AuthRequestDTO;
//import com.example.NIMASA.NYSC.Clearance.Form.DTOs.AuthResponseDTO;
//import com.example.NIMASA.NYSC.Clearance.Form.DTOs.ChangePasswordDTO;
//import com.example.NIMASA.NYSC.Clearance.Form.Enums.UserRole;
//import com.example.NIMASA.NYSC.Clearance.Form.SecurityService.UnifiedAuthService;
//import com.example.NIMASA.NYSC.Clearance.Form.model.Employee;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.security.SecurityRequirement;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.validation.annotation.Validated;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/api/unified-auth")
//@Validated
//@Tag(name = "Unified Authentication", description = "Handles authentication for both employees and corps members")
//public class UnifiedAuthController {
//
//    private final UnifiedAuthService unifiedAuthService;
//
//    @PostMapping("/login")
//    @Operation(summary = "Unified login for employees and corps members")
//    public ResponseEntity<?> login(@Valid @RequestBody AuthRequestDTO request) {
//        try {
//            AuthResponseDTO response = unifiedAuthService.authenticate(request);
//            return ResponseEntity.ok(response);
//        } catch (RuntimeException e) {
//            return ResponseEntity.badRequest().body("Authentication failed: " + e.getMessage());
//        }
//    }
//
//    @PostMapping("/employee/add")
//    @Operation(summary = "Add new employee (Admin only)")
//    @SecurityRequirement(name = "Bearer Authentication")
//    public ResponseEntity<?> addEmployee(
//            @RequestParam String name,
//            @RequestParam String password,
//            @RequestParam String department,
//            @RequestParam UserRole role) {
//        try {
//            Employee employee = unifiedAuthService.addEmployee(name, password, department, role);
//
//
//            employee.setPassword(null);
//
//            return ResponseEntity.ok(employee);
//        } catch (RuntimeException e) {
//            return ResponseEntity.badRequest().body("Failed to add employee: " + e.getMessage());
//        }
//    }
//
//    @PostMapping("/employee/change-password")
//    @Operation(summary = "Change employee password")
//    @SecurityRequirement(name = "Bearer Authentication")
//    public ResponseEntity<?> changeEmployeePassword(@Valid @RequestBody ChangePasswordDTO dto) {
//        try {
//            unifiedAuthService.changeEmployeePassword(dto.getUsername(), dto.getNewPassword());
//            return ResponseEntity.ok("Password changed successfully");
//        } catch (RuntimeException e) {
//            return ResponseEntity.badRequest().body("Password change failed: " + e.getMessage());
//        }
//    }
//
//    @PostMapping("/employee/deactivate")
//    @Operation(summary = "Deactivate employee (Admin only)")
//    @SecurityRequirement(name = "Bearer Authentication")
//    public ResponseEntity<?> deactivateEmployee(@RequestParam String name) {
//        try {
//            unifiedAuthService.deactivateEmployee(name);
//            return ResponseEntity.ok("Employee deactivated successfully");
//        } catch (RuntimeException e) {
//            return ResponseEntity.badRequest().body("Failed to deactivate employee: " + e.getMessage());
//        }
//    }
//
//    @PostMapping("/bootstrap/initial-admin")
//    @Operation(summary = "Create initial admin - only works if no employees exist")
//    public ResponseEntity<?> createInitialAdmin() {
//        try {
//            Employee admin = unifiedAuthService.createInitialAdmin();
//            admin.setPassword(null); // Don't return password
//            return ResponseEntity.ok(admin);
//        } catch (RuntimeException e) {
//            return ResponseEntity.badRequest().body("Failed to create initial admin: " + e.getMessage());
//        }
//    }
//}
package com.example.NIMASA.NYSC.Clearance.Form.controller;

import com.example.NIMASA.NYSC.Clearance.Form.DTOs.AddEmployeeDTO;
import com.example.NIMASA.NYSC.Clearance.Form.DTOs.AuthRequestDTO;
import com.example.NIMASA.NYSC.Clearance.Form.DTOs.AuthResponseDTO;
import com.example.NIMASA.NYSC.Clearance.Form.DTOs.ChangePasswordDTO;
import com.example.NIMASA.NYSC.Clearance.Form.Enums.UserRole;
import com.example.NIMASA.NYSC.Clearance.Form.SecurityService.UnifiedAuthService;
import com.example.NIMASA.NYSC.Clearance.Form.model.Employee;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/unified-auth")
@Validated
@Tag(name = "Unified Authentication", description = "Handles authentication for both employees and corps members")
public class UnifiedAuthController {

    private final UnifiedAuthService unifiedAuthService;

    @PostMapping("/login")
    @Operation(summary = "Unified login for employees and corps members")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequestDTO request, HttpServletResponse response) {
        try {
            AuthResponseDTO authResponse = unifiedAuthService.authenticate(request, response);
            return ResponseEntity.ok(authResponse);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Authentication failed: " + e.getMessage());
        }
    }

    @PostMapping("/employee/add")
    @Operation(summary = "Add new employee (Admin only)")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> addEmployee(@Valid @RequestBody AddEmployeeDTO employeeDTO){
        try {
            Employee employee = unifiedAuthService.addEmployee(
                    employeeDTO.getName(),
                    employeeDTO.getPassword(),
                    employeeDTO.getDepartment(),
                    employeeDTO.getRole()
            );
            employee.setPassword(null);

            return ResponseEntity.ok(employee);
        }
        catch (RuntimeException e){
            return ResponseEntity.badRequest().body("Failed to add employee: " + e.getMessage());
        }

    }

    @PostMapping("/employee/change-password")
    @Operation(summary = "Change employee password")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> changeEmployeePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        try {
            unifiedAuthService.changeEmployeePassword(dto.getUsername(), dto.getNewPassword());
            return ResponseEntity.ok("Password changed successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Password change failed: " + e.getMessage());
        }
    }

    @PostMapping("/employee/deactivate")
    @Operation(summary = "Deactivate employee (Admin only)")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> deactivateEmployee(@RequestParam String name) {
        try {
            unifiedAuthService.deactivateEmployee(name);
            return ResponseEntity.ok("Employee deactivated successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Failed to deactivate employee: " + e.getMessage());
        }
    }

    @PostMapping("/bootstrap/initial-admin")
    @Operation(summary = "Create initial admin - only works if no employees exist")
    public ResponseEntity<?> createInitialAdmin() {
        try {
            Employee admin = unifiedAuthService.createInitialAdmin();
            admin.setPassword(null); // Don't return password
            return ResponseEntity.ok(admin);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Failed to create initial admin: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user by clearing cookie")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        // Clear the auth cookie
        Cookie tokenCookie = new Cookie("authToken", "");
        tokenCookie.setHttpOnly(true);
        tokenCookie.setSecure(false); // Set to true in production with HTTPS
        tokenCookie.setPath("/");
        tokenCookie.setMaxAge(0); // Expire immediately
        response.addCookie(tokenCookie);

        return ResponseEntity.ok("Logged out successfully");
    }
}