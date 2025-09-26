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
public class PrintableFormResponseDTO {
    // Corps Member Information
    private String corpsName;
    private String stateCode;
    private String department;
//    private String cdsDay;

    // Supervisor Information
    private Integer daysAbsent;
    private String conductRemark;
    private String supervisorName;
    private String supervisorSignatureUrl; // URL to signature image
    private LocalDate supervisorDate;

    // HOD Information
    private String hodRemark;
    private String hodName;
    private String hodSignatureUrl; // URL to signature image
    private LocalDate hodDate;

    // Admin Information
    private String adminName;
    private LocalDate approvalDate;
    private FormStatus status;
    private String adminSignatureUrl;
    // Form metadata
    private LocalDate createdAt;
    private UUID formId;
}