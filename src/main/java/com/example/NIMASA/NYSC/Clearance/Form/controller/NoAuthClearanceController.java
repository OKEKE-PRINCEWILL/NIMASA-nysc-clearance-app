//package com.example.NIMASA.NYSC.Clearance.Form.controller;
//
//import com.example.NIMASA.NYSC.Clearance.Form.repository.ApprovedHodRepo;
//import com.example.NIMASA.NYSC.Clearance.Form.model.ApprovedSupervisors;
//import com.example.NIMASA.NYSC.Clearance.Form.service.ClearanceFormService;
//import com.example.NIMASA.NYSC.Clearance.Form.DTOs.*;
//import com.example.NIMASA.NYSC.Clearance.Form.FormStatus;
//import com.example.NIMASA.NYSC.Clearance.Form.model.ApprovedHod;
//import com.example.NIMASA.NYSC.Clearance.Form.repository.ApprovedSupervisorsRepo;
//import com.example.NIMASA.NYSC.Clearance.Form.model.ClearanceForm;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/clearance-forms")
//@RequiredArgsConstructor
//public class ClearanceController {
//    private final ClearanceFormService clearanceFormService;
//    private final ApprovedSupervisorsRepo approvedSupervisorsRepo;
//    private final ApprovedHodRepo approvedHodRepo;
//    @PostMapping
//    public ResponseEntity<ClearanceForm>createForm(@RequestBody ClearanceForm form){
//        return ResponseEntity.ok(clearanceFormService.createForm(form));
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<ClearanceForm> getFormById(@PathVariable Long id){
//        return clearanceFormService.getFormById(id)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
//    }
//
//    @GetMapping
//    public ResponseEntity<List<ClearanceForm>> getAllForms(){
//        return ResponseEntity.ok(clearanceFormService.getAllForms());
//    }
//
//    @GetMapping("/search/corps")
//    public ResponseEntity<List<ClearanceForm>> getCorpsMember(@RequestBody CorpsMemberDTO corpsMemberDTO){
//        return  ResponseEntity.ok(clearanceFormService.getCorpMember(corpsMemberDTO.getCorpsName()));
//    }
//
//    @GetMapping("/search/{supervisorName}")
//    public ResponseEntity< List<ClearanceForm>> getSupervisor (@PathVariable String supervisorName){
//        return ResponseEntity.ok( clearanceFormService.getSupervisor(supervisorName));
//    }
//
//    @GetMapping("/status/{status}")
//    public ResponseEntity< List<ClearanceForm> >getByStatus (@PathVariable FormStatus status) {
//        return ResponseEntity.ok( clearanceFormService.getByStatus(status));
//    }
//
//    @GetMapping("/search/{hodName}")
//    public ResponseEntity< List<ClearanceForm>> getHodName (@PathVariable String hodName) {
//        return ResponseEntity.ok( clearanceFormService.getHodName(hodName));
//    }
//
//    @GetMapping("/supervisor/pending")
//    public ResponseEntity<List<ClearanceForm>> getPendingSupervisorForms() {
//        return ResponseEntity.ok(clearanceFormService.getByStatus(FormStatus.PENDING_SUPERVISOR));
//    }
//
//    @GetMapping("/hod/pending")
//    public ResponseEntity<List<ClearanceForm>> getPendingHodForms() {
//        return ResponseEntity.ok(clearanceFormService.getByStatus(FormStatus.PENDING_HOD));
//    }
//
//    @GetMapping("/admin/pending")
//    public ResponseEntity<List<ClearanceForm>> getPendingAdminForms() {
//        return ResponseEntity.ok(clearanceFormService.getByStatus(FormStatus.PENDING_ADMIN));
//    }
//
//
//    @GetMapping("/search/date-range")
//    public ResponseEntity<List<ClearanceForm>> getFormsBetweenDates(
//            @RequestParam String startDate,
//            @RequestParam String endDate) {
//
//        LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
//        LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);
//
//        return ResponseEntity.ok(clearanceFormService.getFormBetweenDates(start, end));
//    }
//
//    @GetMapping("/count/{status}")
//    public ResponseEntity<Long> countByStatus(@PathVariable FormStatus status) {
//        return ResponseEntity.ok(clearanceFormService.countFormsByStatus(status));
//    }
//
//    @GetMapping("/count/{status}/since/{date}")
//    public ResponseEntity<Long> countByStatusSince(
//            @PathVariable FormStatus status,
//            @PathVariable String date) {
//
//        LocalDateTime sinceDate = LocalDate.parse(date).atStartOfDay();
//        return ResponseEntity.ok(clearanceFormService.countFormByStatusSinceDate(status, sinceDate));
//    }
//
//    @PostMapping(value = "/{id}/supervisor-review", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<ClearanceForm> submitSupervisorReview(
//            @PathVariable Long id,
//            @RequestBody SubmitSupervisorReviewDTO request) {
//
//        return ResponseEntity.ok(
//                clearanceFormService.submitSupervisorReview(
//                        id,
//                        request.getSupervisorName(),
//                        request.getDaysAbsent(),
//                        request.getConductRemark()
//                )
//        );
//    }
//
//    @PostMapping("/{id}/hod-review")
//    public ResponseEntity<ClearanceForm> submitHodReview(
//            @PathVariable Long id,
//            @RequestBody SubmitHodReviewDTO request) {
//
//        return ResponseEntity.ok(
//                clearanceFormService.submitHodReview(
//                        id,
//                        request.getHodRemark(),
//                        request.getHodName()
//                )
//        );
//    }
//
//    @PostMapping("admin/{id}/approve")
//    public ResponseEntity<ClearanceForm> approveForm(
//            @PathVariable Long id,
//            @RequestBody AdminApprovalAndRejectDTO approval) {
//
//        return ResponseEntity.ok(
//                clearanceFormService.approveForm(
//                        id,
//                        approval.getAdminName())
//        );
//    }
//
//    @PostMapping("/admin/{id}/reject")
//    public ResponseEntity<ClearanceForm> rejectForm(
//            @PathVariable Long id,
//            @RequestBody AdminApprovalAndRejectDTO reject) {
//
//        return ResponseEntity.ok(
//                clearanceFormService.rejectForm(
//                        id,
//                        reject.getAdminName())
//        );
//    }
//
//
//    @PostMapping("/admin/supervisors")
//    public ResponseEntity<ApprovedSupervisors> addSupervisor(@RequestBody AddNamesRequestDTO requestDTO) {
//        ApprovedSupervisors supervisor = new ApprovedSupervisors();
//        supervisor.setName(requestDTO.getName());
//        supervisor.setActive(true);
//        return ResponseEntity.ok(approvedSupervisorsRepo.save(supervisor));
//    }
//
//
//    @PostMapping("/admin/hod")
//    public ResponseEntity<ApprovedHod> addHod(@RequestBody AddNamesRequestDTO requestDTO) {
//        ApprovedHod hod = new ApprovedHod();
//        hod.setName(requestDTO.getName());
//        hod.setActive(true);
//        return ResponseEntity.ok(approvedHodRepo.save(hod));
//    }
//
//
//
//}
//
package com.example.NIMASA.NYSC.Clearance.Form.controller;
import com.example.NIMASA.NYSC.Clearance.Form.ResponseFilterService;
import com.example.NIMASA.NYSC.Clearance.Form.repository.ApprovedHodRepo;
import com.example.NIMASA.NYSC.Clearance.Form.model.ApprovedSupervisors;
import com.example.NIMASA.NYSC.Clearance.Form.service.ClearanceFormService;
import com.example.NIMASA.NYSC.Clearance.Form.DTOs.*;
import com.example.NIMASA.NYSC.Clearance.Form.FormStatus;
import com.example.NIMASA.NYSC.Clearance.Form.UserRole;
import com.example.NIMASA.NYSC.Clearance.Form.model.ApprovedHod;
import com.example.NIMASA.NYSC.Clearance.Form.repository.ApprovedSupervisorsRepo;
import com.example.NIMASA.NYSC.Clearance.Form.model.ClearanceForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clearance-forms")
@RequiredArgsConstructor
public class NoAuthClearanceController {

    private final ClearanceFormService clearanceFormService;
    private final ApprovedSupervisorsRepo approvedSupervisorsRepo;
    private final ApprovedHodRepo approvedHodRepo;
    private final ResponseFilterService responseFilterService;

    // Helper method to parse role parameter with default
    private UserRole parseUserRole(String roleParam) {
        if (roleParam == null || roleParam.trim().isEmpty()) {
            return UserRole.CORPS_MEMBER; // Default to most restrictive role
        }
        try {
            return UserRole.valueOf(roleParam.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UserRole.CORPS_MEMBER; // Default on invalid role
        }
    }

    // Corps Member endpoints - No role restriction
    @PostMapping
    public ResponseEntity<CorpsMemberFormResponseDTO> createForm(@Valid @RequestBody CorpsMemberFormRequestDTO requestDTO) {
        ClearanceForm form = new ClearanceForm();
        form.setCorpsName(requestDTO.getCorpsName());
        form.setStateCode(requestDTO.getStateCode());
        form.setDepartment(requestDTO.getDepartment());

        ClearanceForm savedForm = clearanceFormService.createForm(form);

        CorpsMemberFormResponseDTO response = new CorpsMemberFormResponseDTO(
                savedForm.getId(),
                savedForm.getCorpsName(),
                savedForm.getStateCode(),
                savedForm.getDepartment()
        );

        return ResponseEntity.ok(response);
    }

    // Role-based form viewing with role parameter
    @GetMapping("/{id}")
    public ResponseEntity<FilteredClearanceFormResponseDTO> getFormById(
            @PathVariable Long id,
            @RequestParam(value = "role", required = false) String roleParam) {

        return clearanceFormService.getFormById(id)
                .map(form -> {
                    UserRole userRole = parseUserRole(roleParam);
                    FilteredClearanceFormResponseDTO filteredForm = responseFilterService.filterFormByRole(form, userRole);
                    return ResponseEntity.ok(filteredForm);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<FilteredClearanceFormResponseDTO>> getAllForms(
            @RequestParam(value = "role", required = false) String roleParam) {

        List<ClearanceForm> forms = clearanceFormService.getAllForms();
        UserRole userRole = parseUserRole(roleParam);
        List<FilteredClearanceFormResponseDTO> filteredForms = responseFilterService.filterFormsByRole(forms, userRole);

        return ResponseEntity.ok(filteredForms);
    }

    // Role-based status filtering
    @GetMapping("/status/{status}")
    public ResponseEntity<List<FilteredClearanceFormResponseDTO>> getByStatus(
            @PathVariable FormStatus status,
            @RequestParam(value = "role", required = false) String roleParam) {

        List<ClearanceForm> forms = clearanceFormService.getByStatus(status);
        UserRole userRole = parseUserRole(roleParam);
        List<FilteredClearanceFormResponseDTO> filteredForms = responseFilterService.filterFormsByRole(forms, userRole);

        return ResponseEntity.ok(filteredForms);
    }

    // Supervisor-related endpoints with role-based responses
    @GetMapping("/supervisor/pending")
    public ResponseEntity<List<FilteredClearanceFormResponseDTO>> getPendingSupervisorForms(
            @RequestParam(value = "role", required = false) String roleParam) {

        List<ClearanceForm> forms = clearanceFormService.getByStatus(FormStatus.PENDING_SUPERVISOR);
        UserRole userRole = parseUserRole(roleParam);
        List<FilteredClearanceFormResponseDTO> filteredForms = responseFilterService.filterFormsByRole(forms, userRole);

        return ResponseEntity.ok(filteredForms);
    }

    @PostMapping("/{id}/supervisor-review")
    public ResponseEntity<FilteredClearanceFormResponseDTO> submitSupervisorReview(
            @PathVariable Long id,
            @RequestBody SubmitSupervisorReviewDTO request,
            @RequestParam(value = "role", required = false) String roleParam) {

        ClearanceForm form = clearanceFormService.submitSupervisorReview(
                id,
                request.getSupervisorName(),
                request.getDaysAbsent(),
                request.getConductRemark()
        );

        UserRole userRole = parseUserRole(roleParam);
        FilteredClearanceFormResponseDTO filteredForm = responseFilterService.filterFormByRole(form, userRole);

        return ResponseEntity.ok(filteredForm);
    }

    // HOD-related endpoints
    @GetMapping("/hod/pending")
    public ResponseEntity<List<FilteredClearanceFormResponseDTO>> getPendingHodForms(
            @RequestParam(value = "role", required = false) String roleParam) {

        List<ClearanceForm> forms = clearanceFormService.getByStatus(FormStatus.PENDING_HOD);
        UserRole userRole = parseUserRole(roleParam);
        List<FilteredClearanceFormResponseDTO> filteredForms = responseFilterService.filterFormsByRole(forms, userRole);

        return ResponseEntity.ok(filteredForms);
    }

    @PostMapping("/{id}/hod-review")
    public ResponseEntity<FilteredClearanceFormResponseDTO> submitHodReview(
            @PathVariable Long id,
            @RequestBody SubmitHodReviewDTO request,
            @RequestParam(value = "role", required = false) String roleParam) {

        ClearanceForm form = clearanceFormService.submitHodReview(
                id,
                request.getHodName(),
                request.getHodRemark()
        );

        UserRole userRole = parseUserRole(roleParam);
        FilteredClearanceFormResponseDTO filteredForm = responseFilterService.filterFormByRole(form, userRole);

        return ResponseEntity.ok(filteredForm);
    }

    // Admin-related endpoints
    @GetMapping("/admin/pending")
    public ResponseEntity<List<ClearanceForm>> getPendingAdminForms() {
        List<ClearanceForm> forms = clearanceFormService.getByStatus(FormStatus.PENDING_ADMIN);
        return ResponseEntity.ok(forms); // Return full ClearanceForm list, not filtered
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ClearanceForm> approveForm(
            @PathVariable Long id,
            @Valid @RequestBody AdminApprovalAndRejectDTO approval) {

        ClearanceForm form = clearanceFormService.approveForm(id, approval.getAdminName());
        return ResponseEntity.ok(form); // Return full ClearanceForm, not filtered
    }


    @PostMapping("/{id}/reject")
    public ResponseEntity<ClearanceForm> rejectForm(
            @PathVariable Long id,
            @Valid @RequestBody AdminApprovalAndRejectDTO reject) {

        ClearanceForm form = clearanceFormService.rejectForm(id, reject.getAdminName());
        return ResponseEntity.ok(form); // Return full ClearanceForm, not filtered
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteForm(
            @PathVariable Long id,
            @Valid @RequestBody DeleteFormDTO deleteRequest) {

        try {
            if (!clearanceFormService.formExists(id)) {
                return ResponseEntity.notFound().build();
            }

            clearanceFormService.deleteForm(id, deleteRequest.getAdminName());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Form deleted successfully");
            response.put("formId", id);
            response.put("deletedBy", deleteRequest.getAdminName());
            response.put("deletedAt", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Deletion failed: " + e.getMessage());
        }
    }

    // Management endpoints
    @PostMapping("/admin/supervisors")
    public ResponseEntity<ApprovedSupervisors> addSupervisor(@RequestBody AddNamesRequestDTO requestDTO) {
        ApprovedSupervisors supervisor = new ApprovedSupervisors();
        supervisor.setName(requestDTO.getName());
        supervisor.setActive(true);
        return ResponseEntity.ok(approvedSupervisorsRepo.save(supervisor));
    }

    @PostMapping("/admin/hod")
    public ResponseEntity<ApprovedHod> addHod(@RequestBody AddNamesRequestDTO requestDTO) {
        ApprovedHod hod = new ApprovedHod();
        hod.setName(requestDTO.getName());
        hod.setActive(true);
        return ResponseEntity.ok(approvedHodRepo.save(hod));
    }

    // Search endpoints with role filtering
    @GetMapping("/search/corps")
    public ResponseEntity<List<FilteredClearanceFormResponseDTO>> getCorpsMember(
            @RequestBody CorpsMemberFormRequestDTO corpsMemberDTO,
            @RequestParam(value = "role", required = false) String roleParam) {

        List<ClearanceForm> forms = clearanceFormService.getCorpMember(corpsMemberDTO.getCorpsName());
        UserRole userRole = parseUserRole(roleParam);
        List<FilteredClearanceFormResponseDTO> filteredForms = responseFilterService.filterFormsByRole(forms, userRole);

        return ResponseEntity.ok(filteredForms);
    }

    @GetMapping("/search/supervisor/{supervisorName}")
    public ResponseEntity<List<FilteredClearanceFormResponseDTO>> getSupervisor(
            @PathVariable String supervisorName,
            @RequestParam(value = "role", required = false) String roleParam) {

        List<ClearanceForm> forms = clearanceFormService.getSupervisor(supervisorName);
        UserRole userRole = parseUserRole(roleParam);
        List<FilteredClearanceFormResponseDTO> filteredForms = responseFilterService.filterFormsByRole(forms, userRole);

        return ResponseEntity.ok(filteredForms);
    }

    @GetMapping("/search/hod/{hodName}")
    public ResponseEntity<List<FilteredClearanceFormResponseDTO>> getHodName(
            @PathVariable String hodName,
            @RequestParam(value = "role", required = false) String roleParam) {

        List<ClearanceForm> forms = clearanceFormService.getHodName(hodName);
        UserRole userRole = parseUserRole(roleParam);
        List<FilteredClearanceFormResponseDTO> filteredForms = responseFilterService.filterFormsByRole(forms, userRole);

        return ResponseEntity.ok(filteredForms);
    }

    // Utility endpoints
    @GetMapping("/{id}/exists")
    public ResponseEntity<Map<String, Object>> checkFormExists(@PathVariable Long id) {
        boolean exists = clearanceFormService.formExists(id);
        Map<String, Object> response = new HashMap<>();
        response.put("formId", id);
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/count/{status}")
    public ResponseEntity<Long> countByStatus(@PathVariable FormStatus status) {
        return ResponseEntity.ok(clearanceFormService.countFormsByStatus(status));
    }

    // Date range search with role filtering
    @GetMapping("/search/date-range")
    public ResponseEntity<List<FilteredClearanceFormResponseDTO>> getFormsBetweenDates(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(value = "role", required = false) String roleParam) {

        LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);

        List<ClearanceForm> forms = clearanceFormService.getFormBetweenDates(start, end);
        UserRole userRole = parseUserRole(roleParam);
        List<FilteredClearanceFormResponseDTO> filteredForms = responseFilterService.filterFormsByRole(forms, userRole);

        return ResponseEntity.ok(filteredForms);
    }
}