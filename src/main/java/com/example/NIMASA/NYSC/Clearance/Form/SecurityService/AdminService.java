package com.example.NIMASA.NYSC.Clearance.Form.SecurityService;

import com.example.NIMASA.NYSC.Clearance.Form.Security.JwtService;
import com.example.NIMASA.NYSC.Clearance.Form.repository.AdminRepo;
import com.example.NIMASA.NYSC.Clearance.Form.securityModel.Admin;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final AdminRepo adminRepo;
//    private final BCryptPasswordEncoder encoder;
    private final BCryptPasswordEncoder encoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;


    public Admin register (Admin admin){
        if(adminRepo.existsByUsername(admin.getUsername())){
            throw new RuntimeException("Username already exists");
        }
        admin.setPassword(encoder.encode(admin.getPassword()));
        admin.setLastPasswordChange(LocalDate.now());
        return adminRepo.save(admin);
   }
    public String login(String username, String rawPassword) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, rawPassword)
            );
        }catch (BadCredentialsException e) {
            throw new RuntimeException("Invalid credentials");
        }

        Admin admin = adminRepo.findByUsernameAndActive(username, true)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if(admin.getLastPasswordChange().isBefore(LocalDate.now().minusMonths(3))){
            throw new RuntimeException("PASSWORD HAS EXPIRED. PLEASE CHANGE YOUR PASSWORD");

       // if (!encoder.matches(password, admin.getPassword())) {
        //    throw new RuntimeException("Invalid credentials");
        }

        return jwtService.generateToken(username);
    }
    public void changePassword(String username, String newPassword) {
        Admin admin = adminRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        admin.setPassword(encoder.encode(newPassword));
        admin.setLastPasswordChange(LocalDate.now());
        adminRepo.save(admin);
    }
    public Admin findByUsername(String username) {
        return adminRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
    }
    public void deactivateAdmin(String username) {
        Admin admin = findByUsername(username);
        admin.setActive(false);
        adminRepo.save(admin);
    }







}
