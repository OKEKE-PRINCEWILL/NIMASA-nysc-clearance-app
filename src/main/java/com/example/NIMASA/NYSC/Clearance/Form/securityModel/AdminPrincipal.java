package com.example.NIMASA.NYSC.Clearance.Form.securityModel;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@RequiredArgsConstructor
public class AdminPrincipal implements UserDetails {
    private final Admin admin;
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton((new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Override
    public String getPassword() {
        return admin.getPassword() ;
    }

    @Override
    public String getUsername() {
        return admin.getUsername();
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
        return admin.isActive();
    }

    public Admin getAdmin(){
        return admin;
    }

    public String getFullName(){
        return admin.getFullName();
    }

}
