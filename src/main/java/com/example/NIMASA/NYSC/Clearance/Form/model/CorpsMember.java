package com.example.NIMASA.NYSC.Clearance.Form.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "corps_members")
public class CorpsMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private LocalDate createdAt = LocalDate.now();
}