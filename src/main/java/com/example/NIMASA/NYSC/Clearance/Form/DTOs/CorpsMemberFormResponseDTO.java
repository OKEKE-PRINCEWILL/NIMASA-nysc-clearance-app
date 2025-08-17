package com.example.NIMASA.NYSC.Clearance.Form.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CorpsMemberFormResponseDTO {
    private Long id;
    private String corpsName;
    private String stateCode;
    private String department;
}
