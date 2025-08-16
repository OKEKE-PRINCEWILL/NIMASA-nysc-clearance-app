package com.example.NIMASA.NYSC.Clearance.Form.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class SubmitSupervisorReviewDTO {
    private String supervisorName;
    private Integer daysAbsent;
    private String conductRemark;



}
