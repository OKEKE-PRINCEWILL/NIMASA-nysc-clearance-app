package com.example.NIMASA.NYSC.Clearance.Form.DTOs;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CorpsMemberFormRequestDTO {
    @NotBlank(message = "Corps Member Name is required")
    private String corpsName;

    @NotBlank(message = "State Code is required")
    private String stateCode;

    @NotBlank(message = "Department is required")
    private String department;
}