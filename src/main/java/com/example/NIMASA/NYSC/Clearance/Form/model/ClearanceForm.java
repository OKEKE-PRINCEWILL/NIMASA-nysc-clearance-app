package com.example.NIMASA.NYSC.Clearance.Form.model;

import com.example.NIMASA.NYSC.Clearance.Form.FormStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "clear_formstable")
public class ClearanceForm {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "corps_id", nullable = false)
    private CorpsMember corpsMember;
    @NotBlank(message = "Corps Member Name is required")
    @Column(nullable = false)
    private String corpsName;

    @NotBlank(message = "State Code is required")
    @Column(nullable = false)
    private String stateCode;

    @NotBlank(message = "Department is required")
    @Column(nullable = false)
    private String department;


    @Column(nullable = true)
    private Integer dayAbsent;

    @Column(length = 1000, nullable = true)
    private String conductRemark;

    @Column(nullable = true)
    private String supervisorName;

    @Column(nullable = true)
    private String supervisorSignaturePath;

    @Column(nullable = true)
    private LocalDate supervisorDate;


    @Column(length = 1000, nullable = true)
    private String hodRemark;

    @Column(nullable = true)
    private String hodName;

    @Column(nullable = true)
    private String hodSignaturePath;

    @Column(nullable = true)
    private LocalDate hodDate;

    // System fields - required
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FormStatus status;

    @Column(nullable = false)
    private LocalDate createdAt;

    @Column(nullable = false)
    private LocalDate updatedAt;


    @Column(nullable = true)
    private String adminName;

    @Column(nullable = true)
    private LocalDate approvalDate;

    @Column(nullable = true)
    private Boolean approved;
}