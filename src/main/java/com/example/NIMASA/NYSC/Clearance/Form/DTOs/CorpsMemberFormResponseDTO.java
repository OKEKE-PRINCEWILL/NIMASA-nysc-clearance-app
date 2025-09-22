package com.example.NIMASA.NYSC.Clearance.Form.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CorpsMemberFormResponseDTO {

    private String corpsName;
    private String stateCode;
    private String department;
}