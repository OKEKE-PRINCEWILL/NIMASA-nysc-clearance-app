package com.example.NIMASA.NYSC.Clearance.Form.repository;

import com.example.NIMASA.NYSC.Clearance.Form.securityModel.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepo extends JpaRepository<Admin, Long> {
    Optional<Admin> findByUsername(String username);

    Optional<Admin> findByUsernameAndActive(String username, boolean active);

    List<Admin> findByActive(boolean active);


    Optional<Admin> findByFullName(String fullName);

    boolean existsByUsername(String username);
//    List<Admin> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
//            String firstName, String lastName);

}
