
package com.example.NIMASA.NYSC.Clearance.Form.controller;
import com.example.NIMASA.NYSC.Clearance.Form.service.ResponseFilterService;
import com.example.NIMASA.NYSC.Clearance.Form.repository.ApprovedHodRepo;
import com.example.NIMASA.NYSC.Clearance.Form.model.ApprovedSupervisors;
import com.example.NIMASA.NYSC.Clearance.Form.service.ClearanceFormService;
import com.example.NIMASA.NYSC.Clearance.Form.DTOs.*;
import com.example.NIMASA.NYSC.Clearance.Form.FormStatus;
import com.example.NIMASA.NYSC.Clearance.Form.Enums.UserRole;
import com.example.NIMASA.NYSC.Clearance.Form.model.ApprovedHod;
import com.example.NIMASA.NYSC.Clearance.Form.repository.ApprovedSupervisorsRepo;
import com.example.NIMASA.NYSC.Clearance.Form.model.ClearanceForm;
import com.example.NIMASA.NYSC.Clearance.Form.service.SignatureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/clearance-forms")
@RequiredArgsConstructor
public class ClearanceFormController {

    private final ClearanceFormService clearanceFormService;
    private final ApprovedSupervisorsRepo approvedSupervisorsRepo;
    private final ApprovedHodRepo approvedHodRepo;
    private final ResponseFilterService responseFilterService;
    private final SignatureService signatureService;

    //method to parse role parameter with default
    private UserRole parseUserRole(String roleParam) {
        if (roleParam == null || roleParam.trim().isEmpty()) {
            return UserRole.CORPS_MEMBER;
        }
        try {
            return UserRole.valueOf(roleParam.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UserRole.CORPS_MEMBER;
        }
    }

    // Corps Member endpoints
//    @PostMapping
//    public ResponseEntity<CorpsMemberFormResponseDTO> createForm(@Valid @RequestBody CorpsMemberFormRequestDTO requestDTO) {
//        ClearanceForm form = new ClearanceForm();
//        form.setCorpsName(requestDTO.getCorpsName());
//        form.setStateCode(requestDTO.getStateCode());
//        form.setDepartment(requestDTO.getDepartment());
//
//        ClearanceForm savedForm = clearanceFormService.createForm(form);
//
//        CorpsMemberFormResponseDTO response = new CorpsMemberFormResponseDTO(
//                savedForm.getId(),
//                savedForm.getCorpsName(),
//                savedForm.getStateCode(),
//                savedForm.getDepartment()
//        );
//
//        return ResponseEntity.ok(response);
//    }
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


    // Role based form
//    @GetMapping("/{id}")
//    public ResponseEntity<FilteredClearanceFormResponseDTO> getFormById(
//            @PathVariable Long id,
//            @RequestParam(value = "role", required = false) String roleParam) {
//
//        return clearanceFormService.getFormById(id)
//                .map(form -> {
//                    UserRole userRole = parseUserRole(roleParam);
//                    FilteredClearanceFormResponseDTO filteredForm = responseFilterService.filterFormByRole(form, userRole);
//                    return ResponseEntity.ok(filteredForm);
//                })
//                .orElse(ResponseEntity.notFound().build());
//    }
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

    // Role based status filtering
    @GetMapping("/status/{status}")
    public ResponseEntity<List<FilteredClearanceFormResponseDTO>> getByStatus(
            @PathVariable FormStatus status,
            @RequestParam(value = "role", required = false) String roleParam) {

        List<ClearanceForm> forms = clearanceFormService.getByStatus(status);
        UserRole userRole = parseUserRole(roleParam);
        List<FilteredClearanceFormResponseDTO> filteredForms = responseFilterService.filterFormsByRole(forms, userRole);

        return ResponseEntity.ok(filteredForms);
    }

    // Supervisor endpoints
    @GetMapping("/supervisor/pending")
    public ResponseEntity<List<FilteredClearanceFormResponseDTO>> getPendingSupervisorForms(
            @RequestParam(value = "role", required = false) String roleParam) {

        List<ClearanceForm> forms = clearanceFormService.getByStatus(FormStatus.PENDING_SUPERVISOR);
        UserRole userRole = parseUserRole(roleParam);
        List<FilteredClearanceFormResponseDTO> filteredForms = responseFilterService.filterFormsByRole(forms, userRole);

        return ResponseEntity.ok(filteredForms);
    }

//    @PostMapping("/{id}/supervisor-review")
//    public ResponseEntity<FilteredClearanceFormResponseDTO> submitSupervisorReview(
//            @PathVariable Long id,
//            @RequestBody SubmitSupervisorReviewDTO request,
//            @RequestParam(value = "role", required = false) String roleParam) {
//
//        ClearanceForm form = clearanceFormService.submitSupervisorReview(
//                id,
//                request.getSupervisorName(),
//                request.getDaysAbsent(),
//                request.getConductRemark()
//        );
//
//        UserRole userRole = parseUserRole(roleParam);
//        FilteredClearanceFormResponseDTO filteredForm = responseFilterService.filterFormByRole(form, userRole);
//
//        return ResponseEntity.ok(filteredForm);
//    }
@PostMapping("/{id}/supervisor-review")
public ResponseEntity<FilteredClearanceFormResponseDTO> submitSupervisorReview(
        @PathVariable Long id,
        @RequestParam("supervisorName") String supervisorName,
        @RequestParam("daysAbsent") Integer daysAbsent,
        @RequestParam("conductRemark") String conductRemark,
        @RequestParam(value = "signatureFile", required = false) MultipartFile signatureFile,
        @RequestParam(value = "role", required = false) String roleParam) {

    try {
        ClearanceForm form = clearanceFormService.submitSupervisorReview(
                id, supervisorName, daysAbsent, conductRemark, signatureFile);

        UserRole userRole = parseUserRole(roleParam);
        FilteredClearanceFormResponseDTO filteredForm = responseFilterService.filterFormByRole(form, userRole);

        return ResponseEntity.ok(filteredForm);
    } catch (RuntimeException e) {
        return ResponseEntity.badRequest().build();
    }
}


    // HOD endpoints
    @GetMapping("/hod/pending")
    public ResponseEntity<List<FilteredClearanceFormResponseDTO>> getPendingHodForms(
            @RequestParam(value = "role", required = false) String roleParam) {

        List<ClearanceForm> forms = clearanceFormService.getByStatus(FormStatus.PENDING_HOD);
        UserRole userRole = parseUserRole(roleParam);
        List<FilteredClearanceFormResponseDTO> filteredForms = responseFilterService.filterFormsByRole(forms, userRole);

        return ResponseEntity.ok(filteredForms);
    }

//    @PostMapping("/{id}/hod-review")
//    public ResponseEntity<FilteredClearanceFormResponseDTO> submitHodReview(
//            @PathVariable Long id,
//            @RequestBody SubmitHodReviewDTO request,
//            @RequestParam(value = "role", required = false) String roleParam) {
//
//        ClearanceForm form = clearanceFormService.submitHodReview(
//                id,
//                request.getHodName(),
//                request.getHodRemark()
//        );
//
//        UserRole userRole = parseUserRole(roleParam);
//        FilteredClearanceFormResponseDTO filteredForm = responseFilterService.filterFormByRole(form, userRole);
//
//        return ResponseEntity.ok(filteredForm);
//    }
@PostMapping("/{id}/hod-review")
public ResponseEntity<FilteredClearanceFormResponseDTO> submitHodReview(
        @PathVariable Long id,
        @RequestParam("hodName") String hodName,
        @RequestParam("hodRemark") String hodRemark,
        @RequestParam(value = "signatureFile", required = false) MultipartFile signatureFile,
        @RequestParam(value = "role", required = false) String roleParam) {

    try {
        ClearanceForm form = clearanceFormService.submitHodReview(
                id, hodName, hodRemark, signatureFile);

        UserRole userRole = parseUserRole(roleParam);
        FilteredClearanceFormResponseDTO filteredForm = responseFilterService.filterFormByRole(form, userRole);

        return ResponseEntity.ok(filteredForm);
    } catch (RuntimeException e) {
        return ResponseEntity.badRequest().build();
    }
}


    // Admin-related endpoints
    @GetMapping("/admin/pending")
    public ResponseEntity<List<ClearanceForm>> getPendingAdminForms() {
        List<ClearanceForm> forms = clearanceFormService.getByStatus(FormStatus.PENDING_ADMIN);
        return ResponseEntity.ok(forms); // Return full ClearanceForm list not filtered
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ClearanceForm> approveForm(
            @PathVariable Long id,
            @Valid @RequestBody AdminApprovalAndRejectDTO approval) {

        ClearanceForm form = clearanceFormService.approveForm(id, approval.getAdminName());
        return ResponseEntity.ok(form); // Return full ClearanceForm not filtered
    }


    @PostMapping("/{id}/reject")
    public ResponseEntity<ClearanceForm> rejectForm(
            @PathVariable Long id,
            @Valid @RequestBody AdminApprovalAndRejectDTO reject) {

        ClearanceForm form = clearanceFormService.rejectForm(id, reject.getAdminName());
        return ResponseEntity.ok(form); // Return full ClearanceForm not filtered
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
    // New endpoint for corps members to get printable approved forms
    @GetMapping("/{id}/printable")
    public ResponseEntity<PrintableFormResponseDTO> getPrintableForm(
            @PathVariable Long id,
            @RequestParam("corpsName") String corpsName) {

        return clearanceFormService.getPrintableForm(id, corpsName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/signatures/{filename}")
    public ResponseEntity<ByteArrayResource> getSignatureFile(@PathVariable String filename) {
        try {
            byte[] fileData = signatureService.getSignatureFile(filename);
            ByteArrayResource resource = new ByteArrayResource(fileData);

            // Determine content type based on file extension
            String contentType = determineContentType(filename);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
    // New endpoint to get all approved forms for a specific corps member
    @GetMapping("/approved/corps/{corpsName}")
    public ResponseEntity<List<PrintableFormResponseDTO>> getApprovedFormsForCorpsMember(
            @PathVariable String corpsName) {

        List<ClearanceForm> approvedForms = clearanceFormService.getByStatus(FormStatus.APPROVED);
        List<PrintableFormResponseDTO> corpsApprovedForms = approvedForms.stream()
                .filter(form -> form.getCorpsName().equalsIgnoreCase(corpsName))
                .map(form -> clearanceFormService.getPrintableForm(form.getId(), corpsName))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        return ResponseEntity.ok(corpsApprovedForms);
    }
    private String determineContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();

        return switch (extension) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "pdf" -> "application/pdf";
            default -> "application/octet-stream"; // fallback for unknown file types
        };
    }
}