
package com.example.NIMASA.NYSC.Clearance.Form.DTOs;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class SubmitHodReviewDTO {
    @NotBlank(message = "HOD name is required")
    private String hodName;

    @NotBlank(message = "HOD remark is required")
    private String hodRemark;

    private MultipartFile signatureFile; // Add signature file upload
}