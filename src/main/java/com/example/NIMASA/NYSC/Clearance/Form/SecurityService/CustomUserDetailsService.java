package com.example.NIMASA.NYSC.Clearance.Form.SecurityService;

import com.example.NIMASA.NYSC.Clearance.Form.repository.AdminRepo;
import com.example.NIMASA.NYSC.Clearance.Form.securityModel.Admin;
import com.example.NIMASA.NYSC.Clearance.Form.securityModel.AdminPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final AdminRepo adminRepo;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Admin admin = adminRepo.findByUsernameAndActive(username, true)
                .orElseThrow(()-> new UsernameNotFoundException("Admin not Found " + username));
//        if(admin== null){
//            throw new UsernameNotFoundException("Admin not found " + username);
//        }

        return new AdminPrincipal(admin);


}
}
