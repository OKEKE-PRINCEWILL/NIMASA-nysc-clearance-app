package com.example.NIMASA.NYSC.Clearance.Form.DTOs;

import com.example.NIMASA.NYSC.Clearance.Form.Enums.UserRole;
import jdk.jfr.Name;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CurrentUserResponseDTO {
    //Common fields for all users
    private Long id;
    private  String name;
    private String department;
    private UserRole role;
    private String userType;
    private boolean active;
    private LocalDate createdAT;

    // Employee-specific fields (null for corps members)
    private LocalDate lastPasswordChange;
    private boolean passwordExpired;
    private long accessTokenRemainingMinutes;
    private long refreshTokenExpirationMs;
    private String sessionId;
    private long activeSessionCount;

    // Authentication status
    private boolean authenticated;
}
