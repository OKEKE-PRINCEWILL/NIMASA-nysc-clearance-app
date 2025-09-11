
package com.example.NIMASA.NYSC.Clearance.Form.DTOs;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class SubmitSupervisorReviewDTO {
    @NotBlank(message = "Supervisor name is required")
    private String supervisorName;

    @NotNull(message = "Days absent is required")
    @Min(value = 0)
    private Integer daysAbsent;

    @NotBlank(message = "Conduct remark is required")
    private String conductRemark;

    private MultipartFile signatureFile;
    }