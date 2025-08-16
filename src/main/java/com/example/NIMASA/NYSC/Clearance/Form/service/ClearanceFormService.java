package com.example.NIMASA.NYSC.Clearance.Form.service;

import com.example.NIMASA.NYSC.Clearance.Form.repository.ApprovedHodRepo;
import com.example.NIMASA.NYSC.Clearance.Form.repository.ClearanceRepository;
import com.example.NIMASA.NYSC.Clearance.Form.FormStatus;
import com.example.NIMASA.NYSC.Clearance.Form.repository.ApprovedSupervisorsRepo;
import com.example.NIMASA.NYSC.Clearance.Form.model.ClearanceForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClearanceFormService {
    private final ClearanceRepository clearanceRepo;
    private final ApprovedSupervisorsRepo approvedSupervisorsRepo;
    private final ApprovedHodRepo approvedHodRepo;
//    // Add this method to your ClearanceFormService:
//
//    public ClearanceForm submitCorpsMemberForm(String corpsName, String stateCode, String department) {
//        ClearanceForm form = new ClearanceForm();
//
//        // Set corps member data
//        form.setCorpsName(corpsName);
//        form.setStateCode(stateCode);
//        form.setDepartment(department);
//
//        // Set initial status and timestamps
//        form.setStatus(FormStatus.PENDING_SUPERVISOR);
//        form.setCreatedAt(LocalDateTime.now());
//        form.setUpdatedAt(LocalDateTime.now());
//
//        return clearanceRepo.save(form);
//    }

    public ClearanceForm createForm(ClearanceForm form){
        form.setStatus(FormStatus.PENDING_SUPERVISOR);
        form.setCreatedAt(LocalDateTime.now().toLocalDate());
        form.setUpdatedAt(LocalDateTime.now().toLocalDate());
        return clearanceRepo.save(form);
    }

    public Optional<ClearanceForm> getFormById(Long id){
        return clearanceRepo.findById(id);
    }

    public List<ClearanceForm> getAllForms() {
        return clearanceRepo.findAll();
    }


    public List<ClearanceForm> getCorpMember(String corpsName) {
        return clearanceRepo.findByCorpsNameContainingIgnoreCase(corpsName);
    }

    public List<ClearanceForm> getSupervisor (String supervisorName){
        return clearanceRepo.findBySupervisorName(supervisorName);
    }

    public List<ClearanceForm> getByStatus (FormStatus status){
        return clearanceRepo.findByStatus(status);
    }


    public List<ClearanceForm> getHodName (String hodName){
        return clearanceRepo.findByHodName(hodName);
    }

    public List<ClearanceForm> getFormBetweenDates(LocalDateTime start, LocalDateTime end){
        return clearanceRepo.findByCreatedAtBetween(start,end);
    }
    public long countFormsByStatus(FormStatus status){
        return clearanceRepo.countByStatus(status);
    }

    public long countFormByStatusSinceDate(FormStatus status, LocalDateTime date){
        return clearanceRepo.countByStatusAndCreatedAtAfter(status,date);
    }

    public ClearanceForm submitSupervisorReview(Long formId, String supervisorName,
                                                Integer daysAbsent, String conductRemark){

        if(!approvedSupervisorsRepo.existsByNameAndActiveTrue(supervisorName)){
            throw new RuntimeException("Unauthorized Supervisor : " + supervisorName);
        }
        Optional<ClearanceForm> formOpt= clearanceRepo.findById(formId);
        if (formOpt.isEmpty()){
            throw new RuntimeException("Form not found");
        }

        ClearanceForm form= formOpt.get();
        if(form.getStatus() != FormStatus.PENDING_SUPERVISOR){
            throw new RuntimeException("Form not ready for supervisor review");
        }

        form.setSupervisorName(supervisorName);
        form.setDayAbsent(daysAbsent);
        form.setConductRemark(conductRemark);
        form.setSupervisorDate(LocalDateTime.now().toLocalDate());
        form.setUpdatedAt(LocalDateTime.now().toLocalDate());
        form.setStatus(FormStatus.PENDING_HOD);

        return clearanceRepo.save(form);
    }

    public ClearanceForm submitHodReview(Long formId, String hodName, String hodRemark ){
        if (!approvedHodRepo.existsByNameAndActiveTrue(hodName)){
            throw new RuntimeException("Unauthorized HOD : " + hodName);
        }
        Optional<ClearanceForm> formOpt= clearanceRepo.findById(formId);
        if(formOpt.isEmpty()){
            throw new RuntimeException("Form not found");
        }

        ClearanceForm form= formOpt.get();
        if (form.getStatus()!= FormStatus.PENDING_HOD ){
            throw new RuntimeException("Form not ready for HOD review");
        }
        form.setHodName(hodName);
        form.setHodRemark(hodRemark);
        form.setHodDate(LocalDateTime.now().toLocalDate());
        form.setUpdatedAt(LocalDateTime.now().toLocalDate());
        form.setStatus(FormStatus.PENDING_ADMIN);

        return clearanceRepo.save(form);
    }

    public ClearanceForm approveForm(Long formId, String adminName) {
        Optional<ClearanceForm> formOpt = clearanceRepo.findById(formId);
        if (formOpt.isEmpty()) {
            throw new RuntimeException("Form not found");
        }

        ClearanceForm form = formOpt.get();
        if (form.getStatus() != FormStatus.PENDING_ADMIN) {
            throw new RuntimeException("Form not ready for admin approval");
        }

        form.setAdminName(adminName);
        form.setApproved(true);
        form.setApprovalDate(LocalDateTime.now().toLocalDate());
        form.setStatus(FormStatus.APPROVED);
        form.setUpdatedAt(LocalDateTime.now().toLocalDate());

        return clearanceRepo.save(form);
    }


    public ClearanceForm rejectForm(Long formId, String adminName) {
        Optional<ClearanceForm> formOpt = clearanceRepo.findById(formId);
        if (formOpt.isEmpty()) {
            throw new RuntimeException("Form not found");
        }

        ClearanceForm form = formOpt.get();
        form.setAdminName(adminName);
        form.setApproved(false);
        form.setApprovalDate(LocalDateTime.now().toLocalDate());
        form.setStatus(FormStatus.REJECTED);
        form.setUpdatedAt(LocalDateTime.now().toLocalDate());

        return clearanceRepo.save(form);
    }






}
