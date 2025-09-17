
package com.example.NIMASA.NYSC.Clearance.Form.controller;
import com.example.NIMASA.NYSC.Clearance.Form.model.Employee;
import com.example.NIMASA.NYSC.Clearance.Form.securityModel.EmployeePrincipal;
import com.example.NIMASA.NYSC.Clearance.Form.service.ResponseFilterService;
import com.example.NIMASA.NYSC.Clearance.Form.service.ClearanceFormService;
import com.example.NIMASA.NYSC.Clearance.Form.DTOs.*;
import com.example.NIMASA.NYSC.Clearance.Form.FormStatus;
import com.example.NIMASA.NYSC.Clearance.Form.Enums.UserRole;
import com.example.NIMASA.NYSC.Clearance.Form.model.ClearanceForm;
import com.example.NIMASA.NYSC.Clearance.Form.service.SignatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/clearance-forms")
@RequiredArgsConstructor
public class ClearanceFormController {

    private final ClearanceFormService clearanceFormService;
//    private final ApprovedSupervisorsRepo approvedSupervisorsRepo;
//    private final ApprovedHodRepo approvedHodRepo;
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


    @PostMapping("/submission")
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

    @PostMapping(
            value = "/{id}/supervisor-review",                  // your URL stays the same
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE      // 🟢 add this to explicitly tell Spring this endpoint accepts multipart
    )
    public ResponseEntity<FilteredClearanceFormResponseDTO> submitSupervisorReview(
            @PathVariable Long id,
            @Valid @ModelAttribute SubmitSupervisorReviewDTO reviewDTO,
            BindingResult result,
            @RequestParam(value = "role", required = false) String roleParam) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body((FilteredClearanceFormResponseDTO) result.getAllErrors());
        }
        try {
            ClearanceForm form = clearanceFormService.submitSupervisorReview(
                    id,
                    reviewDTO.getSupervisorName(),
                    reviewDTO.getDaysAbsent(),
                    reviewDTO.getConductRemark(),
                    reviewDTO.getSignatureFile());

            UserRole userRole = parseUserRole(roleParam);
            FilteredClearanceFormResponseDTO filteredForm = responseFilterService.filterFormByRole(form, userRole);

            return ResponseEntity.ok(filteredForm);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();

        }
    }

    @PostMapping("/{id}/hod-review")
    public ResponseEntity<FilteredClearanceFormResponseDTO> submitHodReview(
            @PathVariable Long id,
            @Valid @ModelAttribute SubmitHodReviewDTO reviewDTO,
            @RequestParam(value = "role", required = false) String roleParam) {

        try {
            ClearanceForm form = clearanceFormService.submitHodReview(
                    id,
                    reviewDTO.getHodName(),
                    reviewDTO.getHodRemark(),
                    reviewDTO.getSignatureFile()
            );
            UserRole userRole = parseUserRole(roleParam);
            FilteredClearanceFormResponseDTO filteredForm = responseFilterService.filterFormByRole(form, userRole);
            return ResponseEntity.ok(filteredForm);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();

        }
    }


    @GetMapping("/pending")
    public ResponseEntity<List<FilteredClearanceFormResponseDTO>> getPendingFormsForUser(
            @RequestParam(value = "role", required = false) String roleParam) {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            UserRole userRole;
            String userDepartment;

            if (authentication.getPrincipal() instanceof EmployeePrincipal) {
                EmployeePrincipal employeePrincipal = (EmployeePrincipal) authentication.getPrincipal();
                userRole = employeePrincipal.getEmployee().getRole();
                userDepartment = employeePrincipal.getEmployee().getDepartment();
            } else {
                userRole = parseUserRole(roleParam);
                userDepartment = null;
            }
            List<ClearanceForm> pendingForm = clearanceFormService.getPendingFormsForUser(userRole, userDepartment);

            List<FilteredClearanceFormResponseDTO> filteredForms =
                    responseFilterService.filterFormsByRole(pendingForm, userRole);
            return ResponseEntity.ok(filteredForms);

        } catch (Exception e) {
            // Log the error and return bad request
            System.err.println("Error getting pending forms: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/pending/count")
    public ResponseEntity<Map<String, Object>> getPendingCountForUser(){
        try{
            Authentication authentication= SecurityContextHolder.getContext().getAuthentication();

            if(authentication==null || !authentication.isAuthenticated()){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            if(authentication.getPrincipal() instanceof EmployeePrincipal){
                EmployeePrincipal employeePrincipal= (EmployeePrincipal) authentication.getPrincipal();
                UserRole userRole= employeePrincipal.getEmployee().getRole();
                String userDepartment= employeePrincipal.getEmployee().getDepartment();
                long pendingCount= clearanceFormService.getPendingCountForUser(userRole, userDepartment);

                Map<String, Object> response= new HashMap<>();
                response.put("role", userRole);
                response.put("department", userDepartment);
                response.put("pendingCount", pendingCount);

                return ResponseEntity.ok(response);
            }

            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            System.err.println("Error getting pending count: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approveForm(
            @PathVariable Long id,
            @Valid @RequestBody AdminApprovalAndRejectDTO approval) {

        try {
            // Get authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body("Authentication required");
            }

            // Check if user is an admin
            EmployeePrincipal principal = (EmployeePrincipal) authentication.getPrincipal();
            if (principal.getEmployee().getRole() != UserRole.ADMIN) {
                return ResponseEntity.status(403).body("Access denied. Admin role required.");
            }

            // Get admin name from authenticated user
            String adminName = principal.getEmployee().getName();

            ClearanceForm form = clearanceFormService.approveForm(id, adminName);
            return ResponseEntity.ok(form);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Approval failed: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectForm(
            @PathVariable Long id,
            @Valid @RequestBody AdminApprovalAndRejectDTO reject) {

        try {
            // Get authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body("Authentication required");
            }

            // Check if user is an admin
            EmployeePrincipal principal = (EmployeePrincipal) authentication.getPrincipal();
            if (principal.getEmployee().getRole() != UserRole.ADMIN) {
                return ResponseEntity.status(403).body("Access denied. Admin role required.");
            }

            // Get admin name from authenticated user
            String adminName = principal.getEmployee().getName();

            ClearanceForm form = clearanceFormService.rejectForm(id, adminName);
            return ResponseEntity.ok(form);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Rejection failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteForm(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) DeleteFormDTO deleteRequest) {

        try {
            // Get authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body("Authentication required");
            }

            // Check if user is an admin
            EmployeePrincipal principal = (EmployeePrincipal) authentication.getPrincipal();
            if (principal.getEmployee().getRole() != UserRole.ADMIN) {
                return ResponseEntity.status(403).body("Access denied. Admin role required.");
            }

            // Check if form exists
            if (!clearanceFormService.formExists(id)) {
                return ResponseEntity.notFound().build();
            }

            // Get admin name from authenticated user
            String adminName = principal.getEmployee().getName();

            // Delete the form
            clearanceFormService.deleteForm(id, adminName);

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Form deleted successfully");
            response.put("formId", id);
            response.put("deletedBy", adminName);
            response.put("deletedAt", LocalDateTime.now());

            if (deleteRequest != null && deleteRequest.getReason() != null) {
                response.put("reason", deleteRequest.getReason());
            }

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Deletion failed: " + e.getMessage());
        }
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
    @GetMapping("/admin/export/excel")
    @Operation(summary = "Export all forms to Excel (Admin only)")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ByteArrayResource> exportFormsToExcel() {
        try {
            // Get authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).build();
            }

            EmployeePrincipal principal = (EmployeePrincipal) authentication.getPrincipal();
            Employee currentUser = principal.getEmployee();

            // Check if user is admin
            if (currentUser.getRole() != UserRole.ADMIN) {
                return ResponseEntity.status(403).build();
            }

            // Generate Excel file
            byte[] excelData = clearanceFormService.exportFormsToExcel();
            ByteArrayResource resource = new ByteArrayResource(excelData);

            // Generate filename with timestamp
            String filename = "clearance_forms_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd")) + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .contentLength(excelData.length)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
