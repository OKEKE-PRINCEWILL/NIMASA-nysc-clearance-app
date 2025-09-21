package com.example.NIMASA.NYSC.Clearance.Form.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "corps_members")
public class CorpsMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String department;

//    @Column(nullable = false)
//    private String stateCode;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private LocalDate createdAt = LocalDate.now();
}