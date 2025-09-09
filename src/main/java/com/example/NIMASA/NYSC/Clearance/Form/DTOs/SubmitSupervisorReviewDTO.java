
package com.example.NIMASA.NYSC.Clearance.Form.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class SubmitSupervisorReviewDTO {
    private String supervisorName;
    private Integer daysAbsent;
    private String conductRemark;
    private MultipartFile signatureFile;
    }