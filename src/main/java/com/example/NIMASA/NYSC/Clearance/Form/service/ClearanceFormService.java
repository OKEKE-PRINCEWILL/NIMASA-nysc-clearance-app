package com.example.NIMASA.NYSC.Clearance.Form.service;

import com.example.NIMASA.NYSC.Clearance.Form.DTOs.FormTrackingResponseDTO;
import com.example.NIMASA.NYSC.Clearance.Form.DTOs.PrintableFormResponseDTO;
import com.example.NIMASA.NYSC.Clearance.Form.Enums.UserRole;
import com.example.NIMASA.NYSC.Clearance.Form.model.CorpsMember;
import com.example.NIMASA.NYSC.Clearance.Form.model.Employee;
import com.example.NIMASA.NYSC.Clearance.Form.repository.ClearanceRepository;
import com.example.NIMASA.NYSC.Clearance.Form.FormStatus;
import com.example.NIMASA.NYSC.Clearance.Form.model.ClearanceForm;
import com.example.NIMASA.NYSC.Clearance.Form.repository.CorpsMemberRepository;
import com.example.NIMASA.NYSC.Clearance.Form.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * CLEARANCE FORM SERVICE
 * -------------------------------------------------------------
 * This service handles the "life cycle" of a clearance form:
 *   - Creation by corps member
 *   - Review by Supervisor → HOD → Admin
 *   - Approval / Rejection by Admin
 *   - Export for reporting
 *
 * It also manages signatures (file or initials) and enforces
 * role-based checks at every review step.
 */
@Service
@RequiredArgsConstructor
public class ClearanceFormService {

    private final ClearanceRepository clearanceRepo;
    private final EmployeeRepository employeeRepository;
    private final SignatureService signatureService;
    private final CorpsMemberRepository corpsMemberRepository;

    // ============================================================
    // UTILITY → INITIALS GENERATION
    // ============================================================

    private String generateInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "";
        }
        String[] nameParts = fullName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();

        for (int i = 0; i < nameParts.length; i++) {
            if (!nameParts[i].isEmpty()) {
                initials.append(nameParts[i].charAt(0));
                if (i < nameParts.length - 1) {
                    initials.append(".");
                }
            }
        }
        return initials.toString().toUpperCase();
    }


    // ============================================================
    // SUPERVISOR REVIEW
    // ============================================================

    public ClearanceForm submitSupervisorReview(UUID formId, String supervisorName,
                                                Integer daysAbsent, String conductRemark,
                                                MultipartFile signatureFile) {
        Optional<Employee> supervisorOpt = employeeRepository.findByNameAndActive(supervisorName, true);
        if (supervisorOpt.isEmpty()) {
            throw new RuntimeException("Supervisor not found: " + supervisorName);
        }

        Employee supervisor = supervisorOpt.get();
        if (supervisor.getRole() != UserRole.SUPERVISOR) {
            throw new RuntimeException("User is not authorized as supervisor: " + supervisorName);
        }

        ClearanceForm form = clearanceRepo.findById(formId)
                .orElseThrow(() -> new RuntimeException("Form not found"));

        if (!form.getDepartment().equals(supervisor.getDepartment())) {
            throw new RuntimeException("Supervisor can only review forms from their department");
        }
        if (form.getStatus() != FormStatus.PENDING_SUPERVISOR) {
            throw new RuntimeException("Form not ready for supervisor review");
        }

        String signatureUrl;
        try {
            if (signatureFile != null && !signatureFile.isEmpty()) {
                signatureUrl = signatureService.saveSignatureFile(signatureFile, "supervisor", supervisorName);
            } else {
                signatureUrl = generateInitials(supervisorName);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save signature file: " + e.getMessage());
        }

        form.setSupervisorName(supervisorName);
        form.setDayAbsent(daysAbsent);
        form.setConductRemark(conductRemark);
        form.setSupervisorDate(LocalDateTime.now().toLocalDate());
        form.setUpdatedAt(LocalDateTime.now().toLocalDate());
        form.setSupervisorSignaturePath(signatureUrl); // store Cloudinary URL
        form.setStatus(FormStatus.PENDING_HOD);

        return clearanceRepo.save(form);
    }

    // ============================================================
    // HOD REVIEW
    // ============================================================

    public ClearanceForm submitHodReview(UUID formId, String hodName, String hodRemark,
                                         MultipartFile signatureFile) {
        Optional<Employee> hodOpt = employeeRepository.findByNameAndActive(hodName, true);
        if (hodOpt.isEmpty()) {
            throw new RuntimeException("HOD not found: " + hodName);
        }

        Employee hod = hodOpt.get();
        if (hod.getRole() != UserRole.HOD) {
            throw new RuntimeException("User is not authorized as HOD: " + hodName);
        }

        ClearanceForm form = clearanceRepo.findById(formId)
                .orElseThrow(() -> new RuntimeException("Form not found"));

        if (!form.getDepartment().equals(hod.getDepartment())) {
            throw new RuntimeException("HOD can only review forms from their department");
        }
        if (form.getStatus() != FormStatus.PENDING_HOD) {
            throw new RuntimeException("Form not ready for HOD review");
        }

        String signatureUrl;
        try {
            if (signatureFile != null && !signatureFile.isEmpty()) {
                signatureUrl = signatureService.saveSignatureFile(signatureFile, "hod", hodName);
            } else {
                signatureUrl = generateInitials(hodName);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save signature file: " + e.getMessage());
        }

        form.setHodName(hodName);
        form.setHodRemark(hodRemark);
        form.setHodDate(LocalDateTime.now().toLocalDate());
        form.setUpdatedAt(LocalDateTime.now().toLocalDate());
        form.setHodSignaturePath(signatureUrl); // store Cloudinary URL
        form.setStatus(FormStatus.PENDING_ADMIN);

        return clearanceRepo.save(form);
    }

    // ============================================================
    // PRINTABLE VIEW FOR CORPS MEMBER
    // ============================================================

    public Optional<PrintableFormResponseDTO> getPrintableForm(UUID formId, String corpsName) {
        Optional<ClearanceForm> formOpt = clearanceRepo.findById(formId);
        if (formOpt.isEmpty()) return Optional.empty();

        ClearanceForm form = formOpt.get();

        if (form.getStatus() != FormStatus.APPROVED ||
                !form.getCorpsName().equalsIgnoreCase(corpsName)) {
            return Optional.empty();
        }

        PrintableFormResponseDTO dto = new PrintableFormResponseDTO();
        dto.setFormId(form.getId());
        dto.setCorpsName(form.getCorpsName());
        dto.setStateCode(form.getStateCode());
        dto.setDepartment(form.getDepartment());
        dto.setCreatedAt(form.getCreatedAt());

        dto.setDaysAbsent(form.getDayAbsent());
        dto.setConductRemark(form.getConductRemark());
        dto.setSupervisorName(form.getSupervisorName());
        dto.setSupervisorDate(form.getSupervisorDate());
        dto.setSupervisorSignatureUrl(form.getSupervisorSignaturePath()); // already Cloudinary URL or initials

        dto.setHodRemark(form.getHodRemark());
        dto.setHodName(form.getHodName());
        dto.setHodDate(form.getHodDate());
        dto.setHodSignatureUrl(form.getHodSignaturePath()); // already Cloudinary URL or initials

        dto.setAdminName(form.getAdminName());
        dto.setApprovalDate(form.getApprovalDate());
        dto.setStatus(form.getStatus());

        return Optional.of(dto);
    }

    // ============================================================
    // FORM CREATION & BASIC QUERIES
    // ============================================================

    public ClearanceForm createForm(ClearanceForm form) {
        form.setStatus(FormStatus.PENDING_SUPERVISOR);
        form.setCreatedAt(LocalDateTime.now().toLocalDate());
        form.setUpdatedAt(LocalDateTime.now().toLocalDate());
        return clearanceRepo.save(form);
    }

    public Optional<ClearanceForm> getFormById(UUID id) {
        return clearanceRepo.findById(id);
    }

    public List<ClearanceForm> getAllForms() {
        return clearanceRepo.findAll();
    }

    public List<ClearanceForm> getCorpMember(String corpsName) {
        return clearanceRepo.findByCorpsNameContainingIgnoreCase(corpsName);
    }

    public List<ClearanceForm> getSupervisor(String supervisorName) {
        return clearanceRepo.findBySupervisorName(supervisorName);
    }

    public List<ClearanceForm> getByStatus(FormStatus status) {
        return clearanceRepo.findByStatus(status);
    }

    public List<ClearanceForm> getHodName(String hodName) {
        return clearanceRepo.findByHodName(hodName);
    }

    public List<ClearanceForm> getFormBetweenDates(LocalDateTime start, LocalDateTime end) {
        return clearanceRepo.findByCreatedAtBetween(start, end);
    }

    public long countFormsByStatus(FormStatus status) {
        return clearanceRepo.countByStatus(status);
    }

    public boolean formExists(UUID formId) {
        return clearanceRepo.existsById(formId);
    }

    // ============================================================
    // ROLE-BASED PENDING FORMS
    // ============================================================

    public List<ClearanceForm> getPendingFormsForUser(UserRole userRole, String userDepartment) {
        return switch (userRole) {
            case SUPERVISOR -> clearanceRepo.findByStatusAndDepartment(FormStatus.PENDING_SUPERVISOR, userDepartment);


            case HOD -> clearanceRepo.findByStatusAndDepartment(FormStatus.PENDING_HOD, userDepartment);


            case ADMIN -> clearanceRepo.findByStatus(FormStatus.PENDING_ADMIN);


            case CORPS_MEMBER -> List.of();

            default -> throw new IllegalArgumentException("Invalid user role: " + userRole);
        };
    }

    public long getPendingCountForUser(UserRole userRole, String userDepartment) {

        switch (userRole) {
            case SUPERVISOR:
                return clearanceRepo.countByStatusAndDepartment(FormStatus.PENDING_SUPERVISOR, userDepartment);

            case HOD:
                return clearanceRepo.countByStatusAndDepartment(FormStatus.PENDING_HOD, userDepartment);

            case ADMIN:
                return clearanceRepo.countByStatus(FormStatus.PENDING_ADMIN);

            case CORPS_MEMBER:
                return 0L;

            default:
                throw new IllegalArgumentException("Invalid user role: " + userRole);
        }
    }

    // ============================================================
    // TRACKING FORMS
    // ============================================================

    public List<FormTrackingResponseDTO> getFormsForUser(String username, String department, UserRole role) {
        List<ClearanceForm> forms;

        switch (role) {
            case SUPERVISOR -> forms = clearanceRepo.findBySupervisorName(username)
                    .stream().filter(f -> f.getDepartment().equalsIgnoreCase(department)).toList();
            case HOD -> forms = clearanceRepo.findByHodName(username)
                    .stream().filter(f -> f.getDepartment().equalsIgnoreCase(department)).toList();
            case ADMIN -> forms = clearanceRepo.findByAdminName(username);
            default -> throw new RuntimeException("Role not supported for tracking");
        }

        return forms.stream().map(this::mapToDTO).toList();
    }

    private FormTrackingResponseDTO mapToDTO(ClearanceForm form) {
        FormTrackingResponseDTO dto = new FormTrackingResponseDTO();
        dto.setFormId(form.getId());
        dto.setCorpsName(form.getCorpsName());
        dto.setStateCode(form.getStateCode());
        dto.setDepartment(form.getDepartment());
        dto.setStatus(form.getStatus());

        dto.setSupervisorName(form.getSupervisorName());
        dto.setSupervisorDate(form.getSupervisorDate());

        dto.setHodName(form.getHodName());
        dto.setHodDate(form.getHodDate());

        dto.setAdminName(form.getAdminName());
        dto.setApprovalDate(form.getApprovalDate());

        dto.setApproved(form.getApproved());
        return dto;
    }
    // In ClearanceFormService
//    public List<FormTrackingResponseDTO> getFormsForCorps(UUID corpsId) {
//        CorpsMember corps = corpsMemberRepository.findById(corpsId)
//                .orElseThrow(() -> new RuntimeException("Corps member not found"));
//
//        List<ClearanceForm> forms = clearanceRepo.findByCorpsName(corps.getName());
//        return forms.stream().map(this::mapToDTO).toList();
//    }
    public List<FormTrackingResponseDTO> getFormsForCorps(UUID corpsId) {
        CorpsMember corps = corpsMemberRepository.findById(corpsId)
                .orElseThrow(() -> new RuntimeException("Corps member not found"));

        List<ClearanceForm> forms = clearanceRepo.findByCorpsName(corps.getName());
        return forms.stream().map(this::mapToDTO).toList();
    }



    // ============================================================
    // ADMIN APPROVAL & REJECTION
    // ============================================================

    public ClearanceForm approveForm(UUID formId, String adminName) {
        ClearanceForm form = clearanceRepo.findById(formId)
                .orElseThrow(() -> new RuntimeException("Form not found"));

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

    public ClearanceForm rejectForm(UUID formId, String adminName) {
        ClearanceForm form = clearanceRepo.findById(formId)
                .orElseThrow(() -> new RuntimeException("Form not found"));

        form.setAdminName(adminName);
        form.setApproved(false);
        form.setApprovalDate(LocalDateTime.now().toLocalDate());
        form.setStatus(FormStatus.REJECTED);
        form.setUpdatedAt(LocalDateTime.now().toLocalDate());

        return clearanceRepo.save(form);
    }

    public void deleteForm(UUID formId, String adminName) throws IOException {
        ClearanceForm form = clearanceRepo.findById(formId)
                .orElseThrow(() -> new RuntimeException("Form not found with ID: " + formId));

        // Now we delete Cloudinary URLs directly
        if (form.getSupervisorSignaturePath() != null && form.getSupervisorSignaturePath().startsWith("http")) {
            signatureService.deleteSignatureFile(extractPublicId(form.getSupervisorSignaturePath()));
        }
        if (form.getHodSignaturePath() != null && form.getHodSignaturePath().startsWith("http")) {
            signatureService.deleteSignatureFile(extractPublicId(form.getHodSignaturePath()));
        }

        System.out.println("Form with ID " + formId + " deleted by admin: " + adminName +
                " (Corps Member: " + form.getCorpsName() + ") at " + LocalDateTime.now());

        clearanceRepo.deleteById(formId);
    }

    // Helper to get public_id back from Cloudinary URL
    private String extractPublicId(String url) {
        // Example: https://res.cloudinary.com/demo/image/upload/v123456789/signatures/supervisor_JohnDoe_ab12cd34.png
        // We need: signatures/supervisor_JohnDoe_ab12cd34
        int lastSlash = url.lastIndexOf("/");
        int dotIndex = url.lastIndexOf(".");
        return url.substring(url.indexOf("signatures"), dotIndex > lastSlash ? dotIndex : url.length());
    }

    // ============================================================
    // EXPORT → EXCEL REPORTS
    // ============================================================

    public byte[] exportFormsToExcel() throws IOException {
        List<ClearanceForm> allForms = clearanceRepo.findAll();
        List<Employee> allEmployees = employeeRepository.findAll();
        List<CorpsMember> allCorpsMembers = corpsMemberRepository.findAll();

        Workbook workbook = new XSSFWorkbook();
        Sheet formsSheet = workbook.createSheet("Clearance Forms");
        Sheet employeesSheet = workbook.createSheet("Employees");
        Sheet corpsMembersSheet = workbook.createSheet("Corps Members");

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        createClearanceFormsSheet(formsSheet, allForms, headerStyle);
        createEmployeesSheet(employeesSheet, allEmployees, headerStyle);
        createCorpsMembersSheet(corpsMembersSheet, allCorpsMembers, headerStyle);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    private void createClearanceFormsSheet(Sheet sheet, List<ClearanceForm> allForms, CellStyle headerStyle) {
        String[] headers = {
                "ID", "Corps Name", "State Code", "Department", "Status", "Created Date",
                "Days Absent", "Conduct Remark", "Supervisor Name", "Supervisor Date",
                "HOD Name", "HOD Remark", "HOD Date", "Admin Name", "Approval Date", "Approved"
        };
        createHeaderRow(sheet, headers, headerStyle);

        int rowNum = 1;
        for (ClearanceForm form : allForms) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(String.valueOf(form.getId()));
            row.createCell(1).setCellValue(form.getCorpsName());
            row.createCell(2).setCellValue(form.getStateCode());
            row.createCell(3).setCellValue(form.getDepartment());
            row.createCell(4).setCellValue(form.getStatus().toString());
            row.createCell(5).setCellValue(form.getCreatedAt().toString());
            row.createCell(6).setCellValue(form.getDayAbsent() != null ? form.getDayAbsent() : 0);
            row.createCell(7).setCellValue(form.getConductRemark() != null ? form.getConductRemark() : "");
            row.createCell(8).setCellValue(form.getSupervisorName() != null ? form.getSupervisorName() : "");
            row.createCell(9).setCellValue(form.getSupervisorDate() != null ? form.getSupervisorDate().toString() : "");
            row.createCell(10).setCellValue(form.getHodName() != null ? form.getHodName() : "");
            row.createCell(11).setCellValue(form.getHodRemark() != null ? form.getHodRemark() : "");
            row.createCell(12).setCellValue(form.getHodDate() != null ? form.getHodDate().toString() : "");
            row.createCell(13).setCellValue(form.getAdminName() != null ? form.getAdminName() : "");
            row.createCell(14).setCellValue(form.getApprovalDate() != null ? form.getApprovalDate().toString() : "");
            row.createCell(15).setCellValue(form.getApproved() != null ? form.getApproved().toString() : "");
        }
        autoSizeColumns(sheet, headers.length);
    }

    private void createEmployeesSheet(Sheet sheet, List<Employee> allEmployees, CellStyle headerStyle) {
        String[] headers = {"ID", "Name", "Department", "Role", "Active", "Created Date", "Last Password Change"};
        createHeaderRow(sheet, headers, headerStyle);

        int rowNum = 1;
        for (Employee emp : allEmployees) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(String.valueOf(emp.getId()));
            row.createCell(1).setCellValue(emp.getName());
            row.createCell(2).setCellValue(emp.getDepartment());
            row.createCell(3).setCellValue(emp.getRole().toString());
            row.createCell(4).setCellValue(emp.isActive() ? "YES" : "NO");
            row.createCell(5).setCellValue(emp.getCreatedAt().toString());
            row.createCell(6).setCellValue(emp.getLastPasswordChange().toString());
        }
        autoSizeColumns(sheet, headers.length);
    }

    private void createCorpsMembersSheet(Sheet sheet, List<CorpsMember> allCorpsMembers, CellStyle headerStyle) {
        String[] headers = {"ID", "Name", "Department", "Active", "Created Date"};
        createHeaderRow(sheet, headers, headerStyle);

        int rowNum = 1;
        for (CorpsMember cm : allCorpsMembers) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(String.valueOf(cm.getId()));
            row.createCell(1).setCellValue(cm.getName());
            row.createCell(2).setCellValue(cm.getDepartment());
            row.createCell(3).setCellValue(cm.isActive() ? "YES" : "NO");
            row.createCell(4).setCellValue(cm.getCreatedAt().toString());
        }
        autoSizeColumns(sheet, headers.length);
    }

    private void createHeaderRow(Sheet sheet, String[] headers, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void autoSizeColumns(Sheet sheet, int numCols) {
        for (int i = 0; i < numCols; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}



//        package com.example.NIMASA.NYSC.Clearance.Form.service;
//
//import com.example.NIMASA.NYSC.Clearance.Form.DTOs.FormTrackingResponseDTO;
//import com.example.NIMASA.NYSC.Clearance.Form.DTOs.PrintableFormResponseDTO;
//import com.example.NIMASA.NYSC.Clearance.Form.Enums.UserRole;
//import com.example.NIMASA.NYSC.Clearance.Form.model.CorpsMember;
//import com.example.NIMASA.NYSC.Clearance.Form.model.Employee;
//import com.example.NIMASA.NYSC.Clearance.Form.repository.ClearanceRepository;
//import com.example.NIMASA.NYSC.Clearance.Form.FormStatus;
//import com.example.NIMASA.NYSC.Clearance.Form.model.ClearanceForm;
//import com.example.NIMASA.NYSC.Clearance.Form.repository.CorpsMemberRepository;
//import com.example.NIMASA.NYSC.Clearance.Form.repository.EmployeeRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//import org.apache.poi.ss.usermodel.*;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;
//
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
///**
// * CLEARANCE FORM SERVICE
// * -------------------------------------------------------------
// * This service handles the "life cycle" of a clearance form:
// *   - Creation by corps member
// *   - Review by Supervisor → HOD → Admin
// *   - Approval / Rejection by Admin
// *   - Export for reporting
// *
// * It also manages signatures (file or initials) and enforces
// * role-based checks at every review step.
// */
//        @Service
//        @RequiredArgsConstructor
//        public class ClearanceFormService {
//
//            private final ClearanceRepository clearanceRepo;
//            private final EmployeeRepository employeeRepository;
//            private final SignatureService signatureService;
//            private final CorpsMemberRepository corpsMemberRepository;
//
//            // ============================================================
//            // UTILITY → INITIALS GENERATION
//            // ============================================================
//
//            private String generateInitials(String fullName) {
//                if (fullName == null || fullName.trim().isEmpty()) {
//                    return "";
//                }
//                String[] nameParts = fullName.trim().split("\\s+");
//                StringBuilder initials = new StringBuilder();
//
//                for (int i = 0; i < nameParts.length; i++) {
//                    if (!nameParts[i].isEmpty()) {
//                        initials.append(nameParts[i].charAt(0));
//                        if (i < nameParts.length - 1) {
//                            initials.append(".");
//                        }
//                    }
//                }
//                return initials.toString().toUpperCase();
//            }
//
//
//            // ============================================================
//            // SUPERVISOR REVIEW
//            // ============================================================
//
//            public ClearanceForm submitSupervisorReview(UUID formId, String supervisorName,
//                                                        Integer daysAbsent, String conductRemark,
//                                                        MultipartFile signatureFile) {
//                Optional<Employee> supervisorOpt = employeeRepository.findByNameAndActive(supervisorName, true);
//                if (supervisorOpt.isEmpty()) {
//                    throw new RuntimeException("Supervisor not found: " + supervisorName);
//                }
//
//                Employee supervisor = supervisorOpt.get();
//                if (supervisor.getRole() != UserRole.SUPERVISOR) {
//                    throw new RuntimeException("User is not authorized as supervisor: " + supervisorName);
//                }
//
//                ClearanceForm form = clearanceRepo.findById(formId)
//                        .orElseThrow(() -> new RuntimeException("Form not found"));
//
//                if (!form.getDepartment().equals(supervisor.getDepartment())) {
//                    throw new RuntimeException("Supervisor can only review forms from their department");
//                }
//                if (form.getStatus() != FormStatus.PENDING_SUPERVISOR) {
//                    throw new RuntimeException("Form not ready for supervisor review");
//                }
//
//                String signaturePath;
//                try {
//                    if (signatureFile != null && !signatureFile.isEmpty()) {
//                        signaturePath = signatureService.saveSignatureFile(signatureFile, "supervisor", supervisorName);
//                    } else {
//                        signaturePath = generateInitials(supervisorName);
//                    }
//                } catch (IOException e) {
//                    throw new RuntimeException("Failed to save signature file: " + e.getMessage());
//                }
//
//                form.setSupervisorName(supervisorName);
//                form.setDayAbsent(daysAbsent);
//                form.setConductRemark(conductRemark);
//                form.setSupervisorDate(LocalDateTime.now().toLocalDate());
//                form.setUpdatedAt(LocalDateTime.now().toLocalDate());
//                form.setSupervisorSignaturePath(signaturePath);
//                form.setStatus(FormStatus.PENDING_HOD);
//
//                return clearanceRepo.save(form);
//            }
//
//            // ============================================================
//            // HOD REVIEW
//            // ============================================================
//
//            public ClearanceForm submitHodReview(UUID formId, String hodName, String hodRemark,
//                                                 MultipartFile signatureFile) {
//                Optional<Employee> hodOpt = employeeRepository.findByNameAndActive(hodName, true);
//                if (hodOpt.isEmpty()) {
//                    throw new RuntimeException("HOD not found: " + hodName);
//                }
//
//                Employee hod = hodOpt.get();
//                if (hod.getRole() != UserRole.HOD) {
//                    throw new RuntimeException("User is not authorized as HOD: " + hodName);
//                }
//
//                ClearanceForm form = clearanceRepo.findById(formId)
//                        .orElseThrow(() -> new RuntimeException("Form not found"));
//
//                if (!form.getDepartment().equals(hod.getDepartment())) {
//                    throw new RuntimeException("HOD can only review forms from their department");
//                }
//                if (form.getStatus() != FormStatus.PENDING_HOD) {
//                    throw new RuntimeException("Form not ready for HOD review");
//                }
//
//                String signaturePath;
//                try {
//                    if (signatureFile != null && !signatureFile.isEmpty()) {
//                        signaturePath = signatureService.saveSignatureFile(signatureFile, "hod", hodName);
//                    } else {
//                        signaturePath = generateInitials(hodName);
//                    }
//                } catch (IOException e) {
//                    throw new RuntimeException("Failed to save signature file: " + e.getMessage());
//                }
//
//                form.setHodName(hodName);
//                form.setHodRemark(hodRemark);
//                form.setHodDate(LocalDateTime.now().toLocalDate());
//                form.setUpdatedAt(LocalDateTime.now().toLocalDate());
//                form.setHodSignaturePath(signaturePath);
//                form.setStatus(FormStatus.PENDING_ADMIN);
//
//                return clearanceRepo.save(form);
//            }
//
//            // ============================================================
//            // PRINTABLE VIEW FOR CORPS MEMBER
//            // ============================================================
//
//            public Optional<PrintableFormResponseDTO> getPrintableForm(UUID formId, String corpsName) {
//                Optional<ClearanceForm> formOpt = clearanceRepo.findById(formId);
//                if (formOpt.isEmpty()) return Optional.empty();
//
//                ClearanceForm form = formOpt.get();
//
//                if (form.getStatus() != FormStatus.APPROVED ||
//                        !form.getCorpsName().equalsIgnoreCase(corpsName)) {
//                    return Optional.empty();
//                }
//
//                PrintableFormResponseDTO dto = new PrintableFormResponseDTO();
//                dto.setFormId(form.getId());
//                dto.setCorpsName(form.getCorpsName());
//                dto.setStateCode(form.getStateCode());
//                dto.setDepartment(form.getDepartment());
//                dto.setCreatedAt(form.getCreatedAt());
//
//                dto.setDaysAbsent(form.getDayAbsent());
//                dto.setConductRemark(form.getConductRemark());
//                dto.setSupervisorName(form.getSupervisorName());
//                dto.setSupervisorDate(form.getSupervisorDate());
//                dto.setSupervisorSignatureUrl(formatSignatureUrl(form.getSupervisorSignaturePath()));
//
//                dto.setHodRemark(form.getHodRemark());
//                dto.setHodName(form.getHodName());
//                dto.setHodDate(form.getHodDate());
//                dto.setHodSignatureUrl(formatSignatureUrl(form.getHodSignaturePath()));
//
//                dto.setAdminName(form.getAdminName());
//                dto.setApprovalDate(form.getApprovalDate());
//                dto.setStatus(form.getStatus());
//                dto.setAdminSignatureUrl(signatureService.getSignatureUrl("admin_signature.png"));
//
//                return Optional.of(dto);
//            }
//
//            private String formatSignatureUrl(String signaturePath) {
//                if (signaturePath != null && signaturePath.contains("_")) {
//                    return signatureService.getSignatureUrl(signaturePath);
//                }
//                return signaturePath;
//            }
//
//            // ============================================================
//            // FORM CREATION & BASIC QUERIES
//            // ============================================================
//
//            public ClearanceForm createForm(ClearanceForm form) {
//                form.setStatus(FormStatus.PENDING_SUPERVISOR);
//                form.setCreatedAt(LocalDateTime.now().toLocalDate());
//                form.setUpdatedAt(LocalDateTime.now().toLocalDate());
//                return clearanceRepo.save(form);
//            }
//
//            public Optional<ClearanceForm> getFormById(UUID id) {
//                return clearanceRepo.findById(id);
//            }
//
//            public List<ClearanceForm> getAllForms() {
//                return clearanceRepo.findAll();
//            }
//
//            public List<ClearanceForm> getCorpMember(String corpsName) {
//                return clearanceRepo.findByCorpsNameContainingIgnoreCase(corpsName);
//            }
//
//            public List<ClearanceForm> getSupervisor(String supervisorName) {
//                return clearanceRepo.findBySupervisorName(supervisorName);
//            }
//
//            public List<ClearanceForm> getByStatus(FormStatus status) {
//                return clearanceRepo.findByStatus(status);
//            }
//
//            public List<ClearanceForm> getHodName(String hodName) {
//                return clearanceRepo.findByHodName(hodName);
//            }
//            public List<ClearanceForm> getFormBetweenDates(LocalDateTime start, LocalDateTime end){
//                return clearanceRepo.findByCreatedAtBetween(start,end);
//            }
//
//            public long countFormsByStatus(FormStatus status){
//                return clearanceRepo.countByStatus(status);
//            }
//            public boolean formExists(UUID formId) {
//                return clearanceRepo.existsById(formId);
//            }
//
//            // ============================================================
//            // ROLE-BASED PENDING FORMS
//            // ============================================================
//
//            public List<ClearanceForm> getPendingFormsForUser(UserRole userRole, String userDepartment) {
//                return switch (userRole) {
//                    case SUPERVISOR ->
//                            clearanceRepo.findByStatusAndDepartment(FormStatus.PENDING_SUPERVISOR, userDepartment);
//
//
//                    case HOD ->
//                            clearanceRepo.findByStatusAndDepartment(FormStatus.PENDING_HOD, userDepartment);
//
//
//                    case ADMIN ->
//                            clearanceRepo.findByStatus(FormStatus.PENDING_ADMIN);
//
//
//                    case CORPS_MEMBER ->
//                            List.of();
//
//                    default ->
//                            throw new IllegalArgumentException("Invalid user role: " + userRole);
//                };
//            }
//
//            public long getPendingCountForUser(UserRole userRole, String userDepartment){
//
//                switch (userRole){
//                    case SUPERVISOR :
//                        return clearanceRepo.countByStatusAndDepartment(FormStatus.PENDING_SUPERVISOR, userDepartment);
//
//                    case HOD:
//                        return clearanceRepo.countByStatusAndDepartment(FormStatus.PENDING_HOD, userDepartment );
//
//                    case ADMIN:
//                        return clearanceRepo.countByStatus(FormStatus.PENDING_ADMIN);
//
//                    case CORPS_MEMBER:
//                        return 0L;
//
//                    default:
//                        throw new IllegalArgumentException("Invalid user role: " + userRole);
//                }
//            }
//
//            // ============================================================
//            // TRACKING FORMS
//            // ============================================================
//
//            public List<FormTrackingResponseDTO> getFormsForUser(String username, String department, UserRole role) {
//                List<ClearanceForm> forms;
//
//                switch (role) {
//                    case SUPERVISOR -> forms = clearanceRepo.findBySupervisorName(username)
//                            .stream().filter(f -> f.getDepartment().equalsIgnoreCase(department)).toList();
//                    case HOD -> forms = clearanceRepo.findByHodName(username)
//                            .stream().filter(f -> f.getDepartment().equalsIgnoreCase(department)).toList();
//                    case ADMIN -> forms = clearanceRepo.findByAdminName(username);
//                    default -> throw new RuntimeException("Role not supported for tracking");
//                }
//
//                return forms.stream().map(this::mapToDTO).toList();
//            }
//
//            private FormTrackingResponseDTO mapToDTO(ClearanceForm form) {
//                FormTrackingResponseDTO dto = new FormTrackingResponseDTO();
//                dto.setFormId(form.getId());
//                dto.setCorpsName(form.getCorpsName());
//                dto.setStateCode(form.getStateCode());
//                dto.setDepartment(form.getDepartment());
//                dto.setStatus(form.getStatus());
//
//                dto.setSupervisorName(form.getSupervisorName());
//                dto.setSupervisorDate(form.getSupervisorDate());
//
//                dto.setHodName(form.getHodName());
//                dto.setHodDate(form.getHodDate());
//
//                dto.setAdminName(form.getAdminName());
//                dto.setApprovalDate(form.getApprovalDate());
//
//                dto.setApproved(form.getApproved());
//                return dto;
//            }
//
//            // ============================================================
//            // ADMIN APPROVAL & REJECTION
//            // ============================================================
//
//            public ClearanceForm approveForm(UUID formId, String adminName) {
//                ClearanceForm form = clearanceRepo.findById(formId)
//                        .orElseThrow(() -> new RuntimeException("Form not found"));
//
//                if (form.getStatus() != FormStatus.PENDING_ADMIN) {
//                    throw new RuntimeException("Form not ready for admin approval");
//                }
//
//                form.setAdminName(adminName);
//                form.setApproved(true);
//                form.setApprovalDate(LocalDateTime.now().toLocalDate());
//                form.setStatus(FormStatus.APPROVED);
//                form.setUpdatedAt(LocalDateTime.now().toLocalDate());
//
//                return clearanceRepo.save(form);
//            }
//
//            public ClearanceForm rejectForm(UUID formId, String adminName) {
//                ClearanceForm form = clearanceRepo.findById(formId)
//                        .orElseThrow(() -> new RuntimeException("Form not found"));
//
//                form.setAdminName(adminName);
//                form.setApproved(false);
//                form.setApprovalDate(LocalDateTime.now().toLocalDate());
//                form.setStatus(FormStatus.REJECTED);
//                form.setUpdatedAt(LocalDateTime.now().toLocalDate());
//
//                return clearanceRepo.save(form);
//            }
//
//            public void deleteForm(UUID formId, String adminName) {
//                ClearanceForm form = clearanceRepo.findById(formId)
//                        .orElseThrow(() -> new RuntimeException("Form not found with ID: " + formId));
//
//                if (form.getSupervisorSignaturePath() != null && form.getSupervisorSignaturePath().contains("_")) {
//                    signatureService.deleteSignatureFile(form.getSupervisorSignaturePath());
//                }
//                if (form.getHodSignaturePath() != null && form.getHodSignaturePath().contains("_")) {
//                    signatureService.deleteSignatureFile(form.getHodSignaturePath());
//                }
//
//                System.out.println("Form with ID " + formId + " deleted by admin: " + adminName +
//                        " (Corps Member: " + form.getCorpsName() + ") at " + LocalDateTime.now());
//
//                clearanceRepo.deleteById(formId);
//            }
//
//            // ============================================================
//            // EXPORT → EXCEL REPORTS
//            // ============================================================
//
//            public byte[] exportFormsToExcel() throws IOException {
//                List<ClearanceForm> allForms = clearanceRepo.findAll();
//                List<Employee> allEmployees = employeeRepository.findAll();
//                List<CorpsMember> allCorpsMembers = corpsMemberRepository.findAll();
//
//                Workbook workbook = new XSSFWorkbook();
//                Sheet formsSheet = workbook.createSheet("Clearance Forms");
//                Sheet employeesSheet = workbook.createSheet("Employees");
//                Sheet corpsMembersSheet = workbook.createSheet("Corps Members");
//
//                CellStyle headerStyle = workbook.createCellStyle();
//                Font headerFont = workbook.createFont();
//                headerFont.setBold(true);
//                headerFont.setFontHeightInPoints((short) 12);
//                headerStyle.setFont(headerFont);
//                headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
//                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
//
//                createClearanceFormsSheet(formsSheet, allForms, headerStyle);
//                createEmployeesSheet(employeesSheet, allEmployees, headerStyle);
//                createCorpsMembersSheet(corpsMembersSheet, allCorpsMembers, headerStyle);
//
//                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//                workbook.write(outputStream);
//                workbook.close();
//
//                return outputStream.toByteArray();
//            }
//
//            private void createClearanceFormsSheet(Sheet sheet, List<ClearanceForm> allForms, CellStyle headerStyle) {
//                String[] headers = {
//                        "ID", "Corps Name", "State Code", "Department", "Status", "Created Date",
//                        "Days Absent", "Conduct Remark", "Supervisor Name", "Supervisor Date",
//                        "HOD Name", "HOD Remark", "HOD Date", "Admin Name", "Approval Date", "Approved"
//                };
//                createHeaderRow(sheet, headers, headerStyle);
//
//                int rowNum = 1;
//                for (ClearanceForm form : allForms) {
//                    Row row = sheet.createRow(rowNum++);
//                    row.createCell(0).setCellValue(String.valueOf(form.getId()));
//                    row.createCell(1).setCellValue(form.getCorpsName());
//                    row.createCell(2).setCellValue(form.getStateCode());
//                    row.createCell(3).setCellValue(form.getDepartment());
//                    row.createCell(4).setCellValue(form.getStatus().toString());
//                    row.createCell(5).setCellValue(form.getCreatedAt().toString());
//                    row.createCell(6).setCellValue(form.getDayAbsent() != null ? form.getDayAbsent() : 0);
//                    row.createCell(7).setCellValue(form.getConductRemark() != null ? form.getConductRemark() : "");
//                    row.createCell(8).setCellValue(form.getSupervisorName() != null ? form.getSupervisorName() : "");
//                    row.createCell(9).setCellValue(form.getSupervisorDate() != null ? form.getSupervisorDate().toString() : "");
//                    row.createCell(10).setCellValue(form.getHodName() != null ? form.getHodName() : "");
//                    row.createCell(11).setCellValue(form.getHodRemark() != null ? form.getHodRemark() : "");
//                    row.createCell(12).setCellValue(form.getHodDate() != null ? form.getHodDate().toString() : "");
//                    row.createCell(13).setCellValue(form.getAdminName() != null ? form.getAdminName() : "");
//                    row.createCell(14).setCellValue(form.getApprovalDate() != null ? form.getApprovalDate().toString() : "");
//                    row.createCell(15).setCellValue(form.getApproved() != null ? form.getApproved().toString() : "");
//                }
//                autoSizeColumns(sheet, headers.length);
//            }
//
//            private void createEmployeesSheet(Sheet sheet, List<Employee> allEmployees, CellStyle headerStyle) {
//                String[] headers = {"ID", "Name", "Department", "Role", "Active", "Created Date", "Last Password Change"};
//                createHeaderRow(sheet, headers, headerStyle);
//
//                int rowNum = 1;
//                for (Employee emp : allEmployees) {
//                    Row row = sheet.createRow(rowNum++);
//                    row.createCell(0).setCellValue(String.valueOf(emp.getId()));
//                    row.createCell(1).setCellValue(emp.getName());
//                    row.createCell(2).setCellValue(emp.getDepartment());
//                    row.createCell(3).setCellValue(emp.getRole().toString());
//                    row.createCell(4).setCellValue(emp.isActive() ? "YES" : "NO");
//                    row.createCell(5).setCellValue(emp.getCreatedAt().toString());
//                    row.createCell(6).setCellValue(emp.getLastPasswordChange().toString());
//                }
//                autoSizeColumns(sheet, headers.length);
//            }
//
//            private void createCorpsMembersSheet(Sheet sheet, List<CorpsMember> allCorpsMembers, CellStyle headerStyle) {
//                String[] headers = {"ID", "Name", "Department", "Active", "Created Date"};
//                createHeaderRow(sheet, headers, headerStyle);
//
//                int rowNum = 1;
//                for (CorpsMember cm : allCorpsMembers) {
//                    Row row = sheet.createRow(rowNum++);
//                    row.createCell(0).setCellValue(String.valueOf(cm.getId()));
//                    row.createCell(1).setCellValue(cm.getName());
//                    row.createCell(2).setCellValue(cm.getDepartment());
//                    row.createCell(3).setCellValue(cm.isActive() ? "YES" : "NO");
//                    row.createCell(4).setCellValue(cm.getCreatedAt().toString());
//                }
//                autoSizeColumns(sheet, headers.length);
//            }
//
//            private void createHeaderRow(Sheet sheet, String[] headers, CellStyle headerStyle) {
//                Row headerRow = sheet.createRow(0);
//                for (int i = 0; i < headers.length; i++) {
//                    Cell cell = headerRow.createCell(i);
//                    cell.setCellValue(headers[i]);
//                    cell.setCellStyle(headerStyle);
//                }
//            }
//
//            private void autoSizeColumns(Sheet sheet, int numCols) {
//                for (int i = 0; i < numCols; i++) {
//                    sheet.autoSizeColumn(i);
//                }
//            }
//        }
//    }
//}



//        package com.example.NIMASA.NYSC.Clearance.Form.service;
//
//import com.example.NIMASA.NYSC.Clearance.Form.DTOs.FormTrackingResponseDTO;
//import com.example.NIMASA.NYSC.Clearance.Form.DTOs.PrintableFormResponseDTO;
//import com.example.NIMASA.NYSC.Clearance.Form.Enums.UserRole;
//import com.example.NIMASA.NYSC.Clearance.Form.model.CorpsMember;
//import com.example.NIMASA.NYSC.Clearance.Form.model.Employee;
//import com.example.NIMASA.NYSC.Clearance.Form.repository.ClearanceRepository;
//import com.example.NIMASA.NYSC.Clearance.Form.FormStatus;
//import com.example.NIMASA.NYSC.Clearance.Form.model.ClearanceForm;
//import com.example.NIMASA.NYSC.Clearance.Form.repository.CorpsMemberRepository;
//import com.example.NIMASA.NYSC.Clearance.Form.repository.EmployeeRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//import org.apache.poi.ss.usermodel.*;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;
//
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
///**
// * CLEARANCE FORM SERVICE
// * -------------------------------------------------------------
// * This service handles the "life cycle" of a clearance form:
// *   - Creation by corps member
// *   - Review by Supervisor → HOD → Admin
// *   - Approval / Rejection by Admin
// *   - Export for reporting
// *
// * It also manages signatures (file or initials) and enforces
// * role-based checks at every review step.
// */
//        @Service
//        @RequiredArgsConstructor
//        public class ClearanceFormService {
//
//            private final ClearanceRepository clearanceRepo;
//            private final EmployeeRepository employeeRepository;
//            private final SignatureService signatureService;
//            private final CorpsMemberRepository corpsMemberRepository;
//
//            // ============================================================
//            // UTILITY → INITIALS GENERATION
//            // ============================================================
//
//            private String generateInitials(String fullName) {
//                if (fullName == null || fullName.trim().isEmpty()) {
//                    return "";
//                }
//                String[] nameParts = fullName.trim().split("\\s+");
//                StringBuilder initials = new StringBuilder();
//
//                for (int i = 0; i < nameParts.length; i++) {
//                    if (!nameParts[i].isEmpty()) {
//                        initials.append(nameParts[i].charAt(0));
//                        if (i < nameParts.length - 1) {
//                            initials.append(".");
//                        }
//                    }
//                }
//                return initials.toString().toUpperCase();
//            }
//
//
//            // ============================================================
//            // SUPERVISOR REVIEW
//            // ============================================================
//
//            public ClearanceForm submitSupervisorReview(UUID formId, String supervisorName,
//                                                        Integer daysAbsent, String conductRemark,
//                                                        MultipartFile signatureFile) {
//                Optional<Employee> supervisorOpt = employeeRepository.findByNameAndActive(supervisorName, true);
//                if (supervisorOpt.isEmpty()) {
//                    throw new RuntimeException("Supervisor not found: " + supervisorName);
//                }
//
//                Employee supervisor = supervisorOpt.get();
//                if (supervisor.getRole() != UserRole.SUPERVISOR) {
//                    throw new RuntimeException("User is not authorized as supervisor: " + supervisorName);
//                }
//
//                ClearanceForm form = clearanceRepo.findById(formId)
//                        .orElseThrow(() -> new RuntimeException("Form not found"));
//
//                if (!form.getDepartment().equals(supervisor.getDepartment())) {
//                    throw new RuntimeException("Supervisor can only review forms from their department");
//                }
//                if (form.getStatus() != FormStatus.PENDING_SUPERVISOR) {
//                    throw new RuntimeException("Form not ready for supervisor review");
//                }
//
//                String signaturePath;
//                try {
//                    if (signatureFile != null && !signatureFile.isEmpty()) {
//                        signaturePath = signatureService.saveSignatureFile(signatureFile, "supervisor", supervisorName);
//                    } else {
//                        signaturePath = generateInitials(supervisorName);
//                    }
//                } catch (IOException e) {
//                    throw new RuntimeException("Failed to save signature file: " + e.getMessage());
//                }
//
//                form.setSupervisorName(supervisorName);
//                form.setDayAbsent(daysAbsent);
//                form.setConductRemark(conductRemark);
//                form.setSupervisorDate(LocalDateTime.now().toLocalDate());
//                form.setUpdatedAt(LocalDateTime.now().toLocalDate());
//                form.setSupervisorSignaturePath(signaturePath);
//                form.setStatus(FormStatus.PENDING_HOD);
//
//                return clearanceRepo.save(form);
//            }
//
//            // ============================================================
//            // HOD REVIEW
//            // ============================================================
//
//            public ClearanceForm submitHodReview(UUID formId, String hodName, String hodRemark,
//                                                 MultipartFile signatureFile) {
//                Optional<Employee> hodOpt = employeeRepository.findByNameAndActive(hodName, true);
//                if (hodOpt.isEmpty()) {
//                    throw new RuntimeException("HOD not found: " + hodName);
//                }
//
//                Employee hod = hodOpt.get();
//                if (hod.getRole() != UserRole.HOD) {
//                    throw new RuntimeException("User is not authorized as HOD: " + hodName);
//                }
//
//                ClearanceForm form = clearanceRepo.findById(formId)
//                        .orElseThrow(() -> new RuntimeException("Form not found"));
//
//                if (!form.getDepartment().equals(hod.getDepartment())) {
//                    throw new RuntimeException("HOD can only review forms from their department");
//                }
//                if (form.getStatus() != FormStatus.PENDING_HOD) {
//                    throw new RuntimeException("Form not ready for HOD review");
//                }
//
//                String signaturePath;
//                try {
//                    if (signatureFile != null && !signatureFile.isEmpty()) {
//                        signaturePath = signatureService.saveSignatureFile(signatureFile, "hod", hodName);
//                    } else {
//                        signaturePath = generateInitials(hodName);
//                    }
//                } catch (IOException e) {
//                    throw new RuntimeException("Failed to save signature file: " + e.getMessage());
//                }
//
//                form.setHodName(hodName);
//                form.setHodRemark(hodRemark);
//                form.setHodDate(LocalDateTime.now().toLocalDate());
//                form.setUpdatedAt(LocalDateTime.now().toLocalDate());
//                form.setHodSignaturePath(signaturePath);
//                form.setStatus(FormStatus.PENDING_ADMIN);
//
//                return clearanceRepo.save(form);
//            }
//
//            // ============================================================
//            // PRINTABLE VIEW FOR CORPS MEMBER
//            // ============================================================
//
//            public Optional<PrintableFormResponseDTO> getPrintableForm(UUID formId, String corpsName) {
//                Optional<ClearanceForm> formOpt = clearanceRepo.findById(formId);
//                if (formOpt.isEmpty()) return Optional.empty();
//
//                ClearanceForm form = formOpt.get();
//
//                if (form.getStatus() != FormStatus.APPROVED ||
//                        !form.getCorpsName().equalsIgnoreCase(corpsName)) {
//                    return Optional.empty();
//                }
//
//                PrintableFormResponseDTO dto = new PrintableFormResponseDTO();
//                dto.setFormId(form.getId());
//                dto.setCorpsName(form.getCorpsName());
//                dto.setStateCode(form.getStateCode());
//                dto.setDepartment(form.getDepartment());
//                dto.setCreatedAt(form.getCreatedAt());
//
//                dto.setDaysAbsent(form.getDayAbsent());
//                dto.setConductRemark(form.getConductRemark());
//                dto.setSupervisorName(form.getSupervisorName());
//                dto.setSupervisorDate(form.getSupervisorDate());
//                dto.setSupervisorSignatureUrl(formatSignatureUrl(form.getSupervisorSignaturePath()));
//
//                dto.setHodRemark(form.getHodRemark());
//                dto.setHodName(form.getHodName());
//                dto.setHodDate(form.getHodDate());
//                dto.setHodSignatureUrl(formatSignatureUrl(form.getHodSignaturePath()));
//
//                dto.setAdminName(form.getAdminName());
//                dto.setApprovalDate(form.getApprovalDate());
//                dto.setStatus(form.getStatus());
//                dto.setAdminSignatureUrl(signatureService.getSignatureUrl("admin_signature.png"));
//
//                return Optional.of(dto);
//            }
//
//            private String formatSignatureUrl(String signaturePath) {
//                if (signaturePath != null && signaturePath.contains("_")) {
//                    return signatureService.getSignatureUrl(signaturePath);
//                }
//                return signaturePath;
//            }
//
//            // ============================================================
//            // FORM CREATION & BASIC QUERIES
//            // ============================================================
//
//            public ClearanceForm createForm(ClearanceForm form) {
//                form.setStatus(FormStatus.PENDING_SUPERVISOR);
//                form.setCreatedAt(LocalDateTime.now().toLocalDate());
//                form.setUpdatedAt(LocalDateTime.now().toLocalDate());
//                return clearanceRepo.save(form);
//            }
//
//            public Optional<ClearanceForm> getFormById(UUID id) {
//                return clearanceRepo.findById(id);
//            }
//
//            public List<ClearanceForm> getAllForms() {
//                return clearanceRepo.findAll();
//            }
//
//            public List<ClearanceForm> getCorpMember(String corpsName) {
//                return clearanceRepo.findByCorpsNameContainingIgnoreCase(corpsName);
//            }
//
//            public List<ClearanceForm> getSupervisor(String supervisorName) {
//                return clearanceRepo.findBySupervisorName(supervisorName);
//            }
//
//            public List<ClearanceForm> getByStatus(FormStatus status) {
//                return clearanceRepo.findByStatus(status);
//            }
//
//            public List<ClearanceForm> getHodName(String hodName) {
//                return clearanceRepo.findByHodName(hodName);
//            }
//            public List<ClearanceForm> getFormBetweenDates(LocalDateTime start, LocalDateTime end){
//                return clearanceRepo.findByCreatedAtBetween(start,end);
//            }
//
//            public long countFormsByStatus(FormStatus status){
//                return clearanceRepo.countByStatus(status);
//            }
//            public boolean formExists(UUID formId) {
//                return clearanceRepo.existsById(formId);
//            }
//
//            // ============================================================
//            // ROLE-BASED PENDING FORMS
//            // ============================================================
//
//            public List<ClearanceForm> getPendingFormsForUser(UserRole userRole, String userDepartment) {
//                return switch (userRole) {
//                    case SUPERVISOR ->
//                            clearanceRepo.findByStatusAndDepartment(FormStatus.PENDING_SUPERVISOR, userDepartment);
//
//
//                    case HOD ->
//                            clearanceRepo.findByStatusAndDepartment(FormStatus.PENDING_HOD, userDepartment);
//
//
//                    case ADMIN ->
//                            clearanceRepo.findByStatus(FormStatus.PENDING_ADMIN);
//
//
//                    case CORPS_MEMBER ->
//                            List.of();
//
//                    default ->
//                            throw new IllegalArgumentException("Invalid user role: " + userRole);
//                };
//            }
//
//            public long getPendingCountForUser(UserRole userRole, String userDepartment){
//
//                switch (userRole){
//                    case SUPERVISOR :
//                        return clearanceRepo.countByStatusAndDepartment(FormStatus.PENDING_SUPERVISOR, userDepartment);
//
//                    case HOD:
//                        return clearanceRepo.countByStatusAndDepartment(FormStatus.PENDING_HOD, userDepartment );
//
//                    case ADMIN:
//                        return clearanceRepo.countByStatus(FormStatus.PENDING_ADMIN);
//
//                    case CORPS_MEMBER:
//                        return 0L;
//
//                    default:
//                        throw new IllegalArgumentException("Invalid user role: " + userRole);
//                }
//            }
//
//            // ============================================================
//            // TRACKING FORMS
//            // ============================================================
//
//            public List<FormTrackingResponseDTO> getFormsForUser(String username, String department, UserRole role) {
//                List<ClearanceForm> forms;
//
//                switch (role) {
//                    case SUPERVISOR -> forms = clearanceRepo.findBySupervisorName(username)
//                            .stream().filter(f -> f.getDepartment().equalsIgnoreCase(department)).toList();
//                    case HOD -> forms = clearanceRepo.findByHodName(username)
//                            .stream().filter(f -> f.getDepartment().equalsIgnoreCase(department)).toList();
//                    case ADMIN -> forms = clearanceRepo.findByAdminName(username);
//                    default -> throw new RuntimeException("Role not supported for tracking");
//                }
//
//                return forms.stream().map(this::mapToDTO).toList();
//            }
//
//            private FormTrackingResponseDTO mapToDTO(ClearanceForm form) {
//                FormTrackingResponseDTO dto = new FormTrackingResponseDTO();
//                dto.setFormId(form.getId());
//                dto.setCorpsName(form.getCorpsName());
//                dto.setStateCode(form.getStateCode());
//                dto.setDepartment(form.getDepartment());
//                dto.setStatus(form.getStatus());
//
//                dto.setSupervisorName(form.getSupervisorName());
//                dto.setSupervisorDate(form.getSupervisorDate());
//
//                dto.setHodName(form.getHodName());
//                dto.setHodDate(form.getHodDate());
//
//                dto.setAdminName(form.getAdminName());
//                dto.setApprovalDate(form.getApprovalDate());
//
//                dto.setApproved(form.getApproved());
//                return dto;
//            }
//
//            // ============================================================
//            // ADMIN APPROVAL & REJECTION
//            // ============================================================
//
//            public ClearanceForm approveForm(UUID formId, String adminName) {
//                ClearanceForm form = clearanceRepo.findById(formId)
//                        .orElseThrow(() -> new RuntimeException("Form not found"));
//
//                if (form.getStatus() != FormStatus.PENDING_ADMIN) {
//                    throw new RuntimeException("Form not ready for admin approval");
//                }
//
//                form.setAdminName(adminName);
//                form.setApproved(true);
//                form.setApprovalDate(LocalDateTime.now().toLocalDate());
//                form.setStatus(FormStatus.APPROVED);
//                form.setUpdatedAt(LocalDateTime.now().toLocalDate());
//
//                return clearanceRepo.save(form);
//            }
//
//            public ClearanceForm rejectForm(UUID formId, String adminName) {
//                ClearanceForm form = clearanceRepo.findById(formId)
//                        .orElseThrow(() -> new RuntimeException("Form not found"));
//
//                form.setAdminName(adminName);
//                form.setApproved(false);
//                form.setApprovalDate(LocalDateTime.now().toLocalDate());
//                form.setStatus(FormStatus.REJECTED);
//                form.setUpdatedAt(LocalDateTime.now().toLocalDate());
//
//                return clearanceRepo.save(form);
//            }
//
//            public void deleteForm(UUID formId, String adminName) {
//                ClearanceForm form = clearanceRepo.findById(formId)
//                        .orElseThrow(() -> new RuntimeException("Form not found with ID: " + formId));
//
//                if (form.getSupervisorSignaturePath() != null && form.getSupervisorSignaturePath().contains("_")) {
//                    signatureService.deleteSignatureFile(form.getSupervisorSignaturePath());
//                }
//                if (form.getHodSignaturePath() != null && form.getHodSignaturePath().contains("_")) {
//                    signatureService.deleteSignatureFile(form.getHodSignaturePath());
//                }
//
//                System.out.println("Form with ID " + formId + " deleted by admin: " + adminName +
//                        " (Corps Member: " + form.getCorpsName() + ") at " + LocalDateTime.now());
//
//                clearanceRepo.deleteById(formId);
//            }
//
//            // ============================================================
//            // EXPORT → EXCEL REPORTS
//            // ============================================================
//
//            public byte[] exportFormsToExcel() throws IOException {
//                List<ClearanceForm> allForms = clearanceRepo.findAll();
//                List<Employee> allEmployees = employeeRepository.findAll();
//                List<CorpsMember> allCorpsMembers = corpsMemberRepository.findAll();
//
//                Workbook workbook = new XSSFWorkbook();
//                Sheet formsSheet = workbook.createSheet("Clearance Forms");
//                Sheet employeesSheet = workbook.createSheet("Employees");
//                Sheet corpsMembersSheet = workbook.createSheet("Corps Members");
//
//                CellStyle headerStyle = workbook.createCellStyle();
//                Font headerFont = workbook.createFont();
//                headerFont.setBold(true);
//                headerFont.setFontHeightInPoints((short) 12);
//                headerStyle.setFont(headerFont);
//                headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
//                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
//
//                createClearanceFormsSheet(formsSheet, allForms, headerStyle);
//                createEmployeesSheet(employeesSheet, allEmployees, headerStyle);
//                createCorpsMembersSheet(corpsMembersSheet, allCorpsMembers, headerStyle);
//
//                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//                workbook.write(outputStream);
//                workbook.close();
//
//                return outputStream.toByteArray();
//            }
//
//            private void createClearanceFormsSheet(Sheet sheet, List<ClearanceForm> allForms, CellStyle headerStyle) {
//                String[] headers = {
//                        "ID", "Corps Name", "State Code", "Department", "Status", "Created Date",
//                        "Days Absent", "Conduct Remark", "Supervisor Name", "Supervisor Date",
//                        "HOD Name", "HOD Remark", "HOD Date", "Admin Name", "Approval Date", "Approved"
//                };
//                createHeaderRow(sheet, headers, headerStyle);
//
//                int rowNum = 1;
//                for (ClearanceForm form : allForms) {
//                    Row row = sheet.createRow(rowNum++);
//                    row.createCell(0).setCellValue(String.valueOf(form.getId()));
//                    row.createCell(1).setCellValue(form.getCorpsName());
//                    row.createCell(2).setCellValue(form.getStateCode());
//                    row.createCell(3).setCellValue(form.getDepartment());
//                    row.createCell(4).setCellValue(form.getStatus().toString());
//                    row.createCell(5).setCellValue(form.getCreatedAt().toString());
//                    row.createCell(6).setCellValue(form.getDayAbsent() != null ? form.getDayAbsent() : 0);
//                    row.createCell(7).setCellValue(form.getConductRemark() != null ? form.getConductRemark() : "");
//                    row.createCell(8).setCellValue(form.getSupervisorName() != null ? form.getSupervisorName() : "");
//                    row.createCell(9).setCellValue(form.getSupervisorDate() != null ? form.getSupervisorDate().toString() : "");
//                    row.createCell(10).setCellValue(form.getHodName() != null ? form.getHodName() : "");
//                    row.createCell(11).setCellValue(form.getHodRemark() != null ? form.getHodRemark() : "");
//                    row.createCell(12).setCellValue(form.getHodDate() != null ? form.getHodDate().toString() : "");
//                    row.createCell(13).setCellValue(form.getAdminName() != null ? form.getAdminName() : "");
//                    row.createCell(14).setCellValue(form.getApprovalDate() != null ? form.getApprovalDate().toString() : "");
//                    row.createCell(15).setCellValue(form.getApproved() != null ? form.getApproved().toString() : "");
//                }
//                autoSizeColumns(sheet, headers.length);
//            }
//
//            private void createEmployeesSheet(Sheet sheet, List<Employee> allEmployees, CellStyle headerStyle) {
//                String[] headers = {"ID", "Name", "Department", "Role", "Active", "Created Date", "Last Password Change"};
//                createHeaderRow(sheet, headers, headerStyle);
//
//                int rowNum = 1;
//                for (Employee emp : allEmployees) {
//                    Row row = sheet.createRow(rowNum++);
//                    row.createCell(0).setCellValue(String.valueOf(emp.getId()));
//                    row.createCell(1).setCellValue(emp.getName());
//                    row.createCell(2).setCellValue(emp.getDepartment());
//                    row.createCell(3).setCellValue(emp.getRole().toString());
//                    row.createCell(4).setCellValue(emp.isActive() ? "YES" : "NO");
//                    row.createCell(5).setCellValue(emp.getCreatedAt().toString());
//                    row.createCell(6).setCellValue(emp.getLastPasswordChange().toString());
//                }
//                autoSizeColumns(sheet, headers.length);
//            }
//
//            private void createCorpsMembersSheet(Sheet sheet, List<CorpsMember> allCorpsMembers, CellStyle headerStyle) {
//                String[] headers = {"ID", "Name", "Department", "Active", "Created Date"};
//                createHeaderRow(sheet, headers, headerStyle);
//
//                int rowNum = 1;
//                for (CorpsMember cm : allCorpsMembers) {
//                    Row row = sheet.createRow(rowNum++);
//                    row.createCell(0).setCellValue(String.valueOf(cm.getId()));
//                    row.createCell(1).setCellValue(cm.getName());
//                    row.createCell(2).setCellValue(cm.getDepartment());
//                    row.createCell(3).setCellValue(cm.isActive() ? "YES" : "NO");
//                    row.createCell(4).setCellValue(cm.getCreatedAt().toString());
//                }
//                autoSizeColumns(sheet, headers.length);
//            }
//
//            private void createHeaderRow(Sheet sheet, String[] headers, CellStyle headerStyle) {
//                Row headerRow = sheet.createRow(0);
//                for (int i = 0; i < headers.length; i++) {
//                    Cell cell = headerRow.createCell(i);
//                    cell.setCellValue(headers[i]);
//                    cell.setCellStyle(headerStyle);
//                }
//            }
//
//            private void autoSizeColumns(Sheet sheet, int numCols) {
//                for (int i = 0; i < numCols; i++) {
//                    sheet.autoSizeColumn(i);
//                }
//            }
//        }
//    }
//}
