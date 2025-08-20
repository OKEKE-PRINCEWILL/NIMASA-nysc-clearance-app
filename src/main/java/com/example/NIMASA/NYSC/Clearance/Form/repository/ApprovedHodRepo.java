package com.example.NIMASA.NYSC.Clearance.Form.repository;

import com.example.NIMASA.NYSC.Clearance.Form.model.ApprovedHod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApprovedHodRepo extends JpaRepository<ApprovedHod, Long> {
    //Optional<ApprovedHod> findByNameAndActiveTrue(String name);

    boolean existsByNameAndActiveTrue(String name);
}
