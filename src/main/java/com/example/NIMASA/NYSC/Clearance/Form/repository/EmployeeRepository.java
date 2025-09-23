package com.example.NIMASA.NYSC.Clearance.Form.repository;

import com.example.NIMASA.NYSC.Clearance.Form.model.Employee;
import com.example.NIMASA.NYSC.Clearance.Form.Enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {


    Optional<Employee> findByNameAndActive(String name, boolean active);

    Optional<Employee> findByName (String name);

    //boolean existsByName(String name);

    Optional<Employee> findById(UUID employeeId);

    boolean existsByUsername(String username);

    Optional<Employee> findByUsernameIgnoreCaseAndActive(@NotBlank(message = "Name is required") String name, boolean b);
}