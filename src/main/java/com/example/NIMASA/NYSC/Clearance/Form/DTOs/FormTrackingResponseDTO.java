package com.example.NIMASA.NYSC.Clearance.Form.DTOs;

import com.example.NIMASA.NYSC.Clearance.Form.FormStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FormTrackingResponseDTO {
    private UUID formId;
    private UUID corpsId;
    private String corpsName;
    private String stateCode;
    private String department;
    private FormStatus status;

    private String supervisorName;
    private LocalDate supervisorDate;

    private String hodName;
    private LocalDate hodDate;

    private String adminName;
    private LocalDate approvalDate;

    private Boolean approved;
}
