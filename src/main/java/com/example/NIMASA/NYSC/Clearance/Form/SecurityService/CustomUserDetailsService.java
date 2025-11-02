package com.example.NIMASA.NYSC.Clearance.Form.SecurityService;

import com.example.NIMASA.NYSC.Clearance.Form.model.Employee;
import com.example.NIMASA.NYSC.Clearance.Form.repository.EmployeeRepository;
import com.example.NIMASA.NYSC.Clearance.Form.securityModel.EmployeePrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;



@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Employee employee = employeeRepository.findByUsernameIgnoreCaseAndActive(username, true)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        return new EmployeePrincipal(employee);
    }
}
