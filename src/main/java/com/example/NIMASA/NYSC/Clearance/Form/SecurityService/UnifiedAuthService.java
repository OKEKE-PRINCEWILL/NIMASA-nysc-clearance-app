
package com.example.NIMASA.NYSC.Clearance.Form.SecurityService;

import com.example.NIMASA.NYSC.Clearance.Form.model.CorpsMember;
import com.example.NIMASA.NYSC.Clearance.Form.DTOs.AuthRequestDTO;
import com.example.NIMASA.NYSC.Clearance.Form.DTOs.AuthResponseDTO;
import com.example.NIMASA.NYSC.Clearance.Form.model.Employee;
import com.example.NIMASA.NYSC.Clearance.Form.Enums.UserRole;
import com.example.NIMASA.NYSC.Clearance.Form.repository.CorpsMemberRepository;
import com.example.NIMASA.NYSC.Clearance.Form.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.Cookie;
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

    public AuthResponseDTO authenticate(AuthRequestDTO request, HttpServletResponse response) {
        Optional<Employee> employeeOpt = employeeRepository.findByNameAndActive(request.getName(), true);

        if (employeeOpt.isPresent()) {
            Employee employee = employeeOpt.get();

            // check if password was provided
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return createPasswordRequiredResponse(employee);
            }

            // validate password
            if (!encoder.matches(request.getPassword(), employee.getPassword())) {
                throw new RuntimeException("Invalid password. Please provide the correct password.");
            }

            if (employee.getLastPasswordChange().isBefore(LocalDate.now().minusMonths(3))) {
                throw new RuntimeException("Password has expired. Please change your password.");
            }

            String token = jwtService.generateToken(employee.getName());

            return createEmployeeSuccessResponse(employee, token, response);
        }
        else {
            return handleCorpsMember(request);
        }
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
            // Create new corps member
            CorpsMember newCorpsMember = new CorpsMember();
            newCorpsMember.setName(request.getName());
            newCorpsMember.setDepartment(request.getDepartment());
            newCorpsMember.setActive(true);
            newCorpsMember.setCreatedAt(LocalDate.now());

            CorpsMember savedCorpsMember =  corpsMemberRepository.save(newCorpsMember);
            return createCorpsMemberResponse(savedCorpsMember, true);
        }
    }

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

    private AuthResponseDTO createEmployeeSuccessResponse(Employee employee, String token, HttpServletResponse response) {
        // Add token to cookie
        Cookie tokenCookie = new Cookie("authToken", token);
        tokenCookie.setHttpOnly(true);
        tokenCookie.setSecure(false); // Set to true in production with HTTPS
        tokenCookie.setPath("/");
        tokenCookie.setMaxAge(86400); // 24 hours (same as JWT expiration)
        response.addCookie(tokenCookie);

        AuthResponseDTO authResponse = new AuthResponseDTO();
        authResponse.setMessage("Employee authentication successful");
        authResponse.setName(employee.getName());
        authResponse.setDepartment(employee.getDepartment());
        authResponse.setRole(employee.getRole());
        authResponse.setUserType("EMPLOYEE");
        authResponse.setToken(token);
        authResponse.setPasswordRequired(false);
        authResponse.setNewCorpsMember(false);
        return authResponse;
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

    // method to add employees for admin only
    public Employee addEmployee(String name, String password, String department, UserRole role) {
        if (employeeRepository.existsByName(name)) {
            throw new RuntimeException("Employee with this name already exists");
        }

        // to validate roles
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

    // my method to change password
    public void changeEmployeePassword(String name, String newPassword) {
        Employee employee = employeeRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        employee.setPassword(encoder.encode(newPassword));
        employee.setLastPasswordChange(LocalDate.now());
        employeeRepository.save(employee);
    }

    // my deactivate employee method
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
}