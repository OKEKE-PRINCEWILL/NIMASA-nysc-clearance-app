package com.example.NIMASA.NYSC.Clearance.Form.DTOs;

import com.example.NIMASA.NYSC.Clearance.Form.Enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class AddEmployeeDTO {
    @NotBlank(message = "Employee name is required" )
    private String name;

    @NotBlank(message= "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Department is required")
    private String department;

    @NotNull(message = "Role is required")
    private UserRole role;
}
