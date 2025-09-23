package com.example.NIMASA.NYSC.Clearance.Form.DTOs;

import com.example.NIMASA.NYSC.Clearance.Form.Enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Struct;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class AuthResponseDTO {
    private UUID id;
    private String username;
    private String message;
    private String name;
    private String department;
    private String UserType;
    private UserRole role;
//    private String token;
    private boolean NewCorpsMember;
    private boolean passwordRequired;

    private long accessTokenExpirationMs;
    private boolean requiresRefresh;
}
