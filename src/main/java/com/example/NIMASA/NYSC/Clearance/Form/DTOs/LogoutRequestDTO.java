package com.example.NIMASA.NYSC.Clearance.Form.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogoutRequestDTO {
    private boolean logoutAllDevices = false; // Logout from all sessions
}