package com.example.NIMASA.NYSC.Clearance.Form.DTOs;

import com.example.NIMASA.NYSC.Clearance.Form.Enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeListResponseDTO {
    private UUID id;
    private String name;
    private String username;
    private String department;
    private UserRole userRole;
    private boolean active;
    private LocalDate createdAT;
    private LocalDate lastPasswordChange;
    private boolean passwordExpired;
    private long formPendingReview;
}
