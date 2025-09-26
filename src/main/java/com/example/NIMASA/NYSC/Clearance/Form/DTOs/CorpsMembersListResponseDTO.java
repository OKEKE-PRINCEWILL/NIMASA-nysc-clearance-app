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
public class CorpsMembersListResponseDTO {
    private UUID id;
    private String name;
    private String department;
    //private String cdsDay;
    private boolean active;
    private LocalDate createdAT;
//    private long formCount;


}
