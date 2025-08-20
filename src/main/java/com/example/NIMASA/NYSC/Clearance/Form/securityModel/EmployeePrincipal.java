package com.example.NIMASA.NYSC.Clearance.Form.securityModel;

import com.example.NIMASA.NYSC.Clearance.Form.model.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@RequiredArgsConstructor
public class EmployeePrincipal implements UserDetails {
    private final Employee employee;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_" + employee.getRole().name()));
    }

    @Override
    public String getPassword() {
        return employee.getPassword();
    }

    @Override
    public String getUsername() {
        return employee.getName();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return employee.isActive();
    }

    public Employee getEmployee() {
        return employee;
    }

    public String getFullName() {
        return employee.getName();
    }

    public String getDepartment() {
        return employee.getDepartment();
    }
}