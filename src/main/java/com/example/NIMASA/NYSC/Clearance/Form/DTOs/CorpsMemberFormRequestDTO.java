package com.example.NIMASA.NYSC.Clearance.Form.DTOs;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CorpsMemberFormRequestDTO {

    private UUID corpsMemberId;

    @NotBlank(message = "Corps Member Name is required")
    private String corpsName;

    @NotBlank(message = "State Code is required")
    private String stateCode;

    @NotBlank(message = "Department is required")
    private String department;
}