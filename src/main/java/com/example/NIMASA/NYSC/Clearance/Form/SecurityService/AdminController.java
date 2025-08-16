package com.example.NIMASA.NYSC.Clearance.Form.SecurityService;

import com.example.NIMASA.NYSC.Clearance.Form.DTOs.ChangePasswordDTO;
import com.example.NIMASA.NYSC.Clearance.Form.DTOs.LoginRequestDTO;
import com.example.NIMASA.NYSC.Clearance.Form.DTOs.RegisterRequestDto;
import com.example.NIMASA.NYSC.Clearance.Form.securityModel.Admin;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Validated
public class AdminController {
    private final  AdminService adminService;

    @PostMapping("/register")
    public ResponseEntity<?> register (@Valid @RequestBody RegisterRequestDto requestDto){
       try {
           Admin admin = new Admin();
           admin.setPassword(requestDto.getPassword());
           admin.setUsername(requestDto.getUsername());
           admin.setFullName(requestDto.getFullName());

           Admin savedAdmin = adminService.register(admin);

           savedAdmin.setPassword(null);
           return ResponseEntity.ok(savedAdmin);
       }catch (RuntimeException e){
           return ResponseEntity.badRequest().body("Registration failed: " + e.getMessage());
       }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO requestDTO){
        try{Admin admin = new Admin();
        admin.setUsername(requestDTO.getUsername());
        admin.setPassword(requestDTO.getPassword());
        return ResponseEntity.ok( adminService.login(admin.getUsername(), admin.getPassword()));
    }catch (RuntimeException e){
            return ResponseEntity.badRequest().body("Login failed " + e.getMessage());
        }
    }


    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        try {
            adminService.changePassword((dto.getUsername()), dto.getNewPassword());
            return ResponseEntity.ok("Password changed successfully");
        } catch (RuntimeException e){
            return ResponseEntity.badRequest().body("Password change failed: " + e.getMessage());
        }
    }
}
