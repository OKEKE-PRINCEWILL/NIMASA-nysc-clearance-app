package com.example.NIMASA.NYSC.Clearance.Form.DTOs;

import com.example.NIMASA.NYSC.Clearance.Form.FormStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FilteredClearanceFormResponseDTO {
    private UUID id;
    private String corpsName;
    private String stateCode;
    private String department;
    private FormStatus status;
    private LocalDate createdAt;
    private LocalDate updatedAt;

    // Supervisor fields - only visible to SUPERVISOR, HOD, ADMIN
    private Integer dayAbsent;
    private String conductRemark;
    private String supervisorName;
    private String supervisorSignaturePath;
    private LocalDate supervisorDate;

    // HOD fields - only visible to HOD, ADMIN
    private String hodRemark;
    private String hodName;
    private String hodSignaturePath;
    private LocalDate hodDate;

    // Admin fields - only visible to ADMIN
    private String adminName;
    private LocalDate approvalDate;
    private Boolean approved;
}

// 3. Response Filter Service
