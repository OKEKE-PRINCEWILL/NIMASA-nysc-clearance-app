package com.example.NIMASA.NYSC.Clearance.Form.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminApprovalAndRejectDTO {
    private String token;
    private String adminName;
    private String message;
}
