package com.example.NIMASA.NYSC.Clearance.Form.SecurityService;

import com.example.NIMASA.NYSC.Clearance.Form.model.Employee;
//import com.example.NIMASA.NYSC.Clearance.Form.repository.AdminRepo;
import com.example.NIMASA.NYSC.Clearance.Form.repository.EmployeeRepository;
//import com.example.NIMASA.NYSC.Clearance.Form.model.Admin;
//import com.example.NIMASA.NYSC.Clearance.Form.securityModel.AdminPrincipal;
import com.example.NIMASA.NYSC.Clearance.Form.securityModel.EmployeePrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {
//    private final AdminRepo adminRepo;
    private final EmployeeRepository employeeRepository;

   @Override
 public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {


        // Then check if it's an employee (using name as username)
        Optional<Employee> employeeOpt = employeeRepository.findByNameAndActive(username, true);
        if (employeeOpt.isPresent()) {
            return new EmployeePrincipal(employeeOpt.get());
        }

        throw new UsernameNotFoundException("User not found: " + username);
    }
}