package com.example.NIMASA.NYSC.Clearance.Form.repository;

import com.example.NIMASA.NYSC.Clearance.Form.model.Employee;
import com.example.NIMASA.NYSC.Clearance.Form.Enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByNameAndActive(String name, boolean active);

    Optional<Employee> findByName (String name);

    boolean existsByName(String name);


//    List<Employee> findByRole(UserRole role);
//
//    List<Employee> findByRoleAndActive(UserRole role, boolean active);
//
//    List<Employee> findByDepartment(String department);
//
//    boolean existsByNameAndActive(String name, boolean active);
}
