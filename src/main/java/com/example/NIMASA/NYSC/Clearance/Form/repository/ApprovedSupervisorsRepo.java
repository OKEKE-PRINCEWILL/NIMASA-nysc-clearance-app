package com.example.NIMASA.NYSC.Clearance.Form.repository;

import com.example.NIMASA.NYSC.Clearance.Form.model.ApprovedSupervisors;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApprovedSupervisorsRepo extends JpaRepository<ApprovedSupervisors,Long> {
//    Optional<ApprovedSupervisors> findByNameAndActiveTrue(String name);

    boolean existsByNameAndActiveTrue(String name);
}
