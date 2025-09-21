package com.example.NIMASA.NYSC.Clearance.Form.repository;

import com.example.NIMASA.NYSC.Clearance.Form.FormStatus;
import com.example.NIMASA.NYSC.Clearance.Form.model.ClearanceForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ClearanceRepository extends JpaRepository<ClearanceForm, UUID> {

    List<ClearanceForm> findByStatus(FormStatus status);

    List<ClearanceForm> findByCorpsNameContainingIgnoreCase(String corpsName);

    List<ClearanceForm> findBySupervisorName(String supervisorName);

    List<ClearanceForm> findByHodName(String hodName);

    List<ClearanceForm> findByCorpsName(String corpsName);

    List<ClearanceForm> findByCorpsMember_Id(UUID corpsId);


    List<ClearanceForm> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByStatus(FormStatus status);

    long countByStatusAndCreatedAtAfter(FormStatus status, LocalDateTime date);

    List<ClearanceForm> findByStatusAndDepartment(FormStatus status, String department);

    long countByStatusAndDepartment(FormStatus status, String department);

    List<ClearanceForm> findByAdminName(String adminName);
}

