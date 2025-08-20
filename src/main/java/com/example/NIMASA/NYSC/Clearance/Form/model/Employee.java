package com.example.NIMASA.NYSC.Clearance.Form.model;

import com.example.NIMASA.NYSC.Clearance.Form.Enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private LocalDate createdAt = LocalDate.now();

    @Column(nullable = false)
    private LocalDate lastPasswordChange = LocalDate.now();


}