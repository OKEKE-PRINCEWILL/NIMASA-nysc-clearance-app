
package com.example.NIMASA.NYSC.Clearance.Form.service;

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
import org.hibernate.grammars.hql.HqlParser;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClearanceFormService {
    private final ClearanceRepository clearanceRepo;
//    private final ApprovedSupervisorsRepo approvedSupervisorsRepo;
//    private final ApprovedHodRepo approvedHodRepo;
    private final EmployeeRepository employeeRepository;
    private final SignatureService signatureService;
    private final CorpsMemberRepository corpsMemberRepository;// Add signature service

    // Generate initials as fallback (keep existing method)
    private String generateInitials(String fullName){
        if(fullName== null || fullName.trim().isEmpty()){
            return "";
        }
        String[] nameParts= fullName.trim().split("\\s+");
        StringBuilder initials= new StringBuilder();

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

    // Enhanced supervisor review with signature file
    public ClearanceForm submitSupervisorReview(Long formId, String supervisorName,
                                                Integer daysAbsent, String conductRemark,
                                                MultipartFile signatureFile){

        // SIMPLIFIED: Just check if supervisor exists as active employee with SUPERVISOR role
        Optional<Employee> supervisorOpt = employeeRepository.findByNameAndActive(supervisorName, true);

        if (supervisorOpt.isEmpty()) {
            throw new RuntimeException("Supervisor not found: " + supervisorName);
        }

        Employee supervisor = supervisorOpt.get();
        if (supervisor.getRole() != UserRole.SUPERVISOR) {
            throw new RuntimeException("User is not authorized as supervisor: " + supervisorName);
        }

        // BONUS: Check if supervisor is from same department as the form
        Optional<ClearanceForm> formOpt = clearanceRepo.findById(formId);
        if (formOpt.isEmpty()) {
            throw new RuntimeException("Form not found");
        }

        ClearanceForm form = formOpt.get();
        if (!form.getDepartment().equals(supervisor.getDepartment())) {
            throw new RuntimeException("Supervisor can only review forms from their department");
        }

        if (form.getStatus() != FormStatus.PENDING_SUPERVISOR) {
            throw new RuntimeException("Form not ready for supervisor review");
        }

        // Handle signature file or fallback to initials
        String signaturePath;
        try {
            if (signatureFile != null && !signatureFile.isEmpty()) {
                signaturePath = signatureService.saveSignatureFile(signatureFile, "supervisor", supervisorName);
            } else {
                signaturePath = generateInitials(supervisorName);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save signature file: " + e.getMessage());
        }

        form.setSupervisorName(supervisorName);
        form.setDayAbsent(daysAbsent);
        form.setConductRemark(conductRemark);
        form.setSupervisorDate(LocalDateTime.now().toLocalDate());
        form.setUpdatedAt(LocalDateTime.now().toLocalDate());
        form.setSupervisorSignaturePath(signaturePath);
        form.setStatus(FormStatus.PENDING_HOD);

        return clearanceRepo.save(form);
    }

    // Enhanced HOD review with signature file
    public ClearanceForm submitHodReview(Long formId, String hodName, String hodRemark,
                                         MultipartFile signatureFile){

        // SIMPLIFIED: Just check if HOD exists as active employee with HOD role
        Optional<Employee> hodOpt = employeeRepository.findByNameAndActive(hodName, true);

        if (hodOpt.isEmpty()) {
            throw new RuntimeException("HOD not found: " + hodName);
        }

        Employee hod = hodOpt.get();
        if (hod.getRole() != UserRole.HOD) {
            throw new RuntimeException("User is not authorized as HOD: " + hodName);
        }

        Optional<ClearanceForm> formOpt = clearanceRepo.findById(formId);
        if (formOpt.isEmpty()) {
            throw new RuntimeException("Form not found");
        }

        ClearanceForm form = formOpt.get();

        // BONUS: Check if HOD is from same department as the form
        if (!form.getDepartment().equals(hod.getDepartment())) {
            throw new RuntimeException("HOD can only review forms from their department");
        }

        if (form.getStatus() != FormStatus.PENDING_HOD) {
            throw new RuntimeException("Form not ready for HOD review");
        }

        // Handle signature file or fallback to initials
        String signaturePath;
        try {
            if (signatureFile != null && !signatureFile.isEmpty()) {
                signaturePath = signatureService.saveSignatureFile(signatureFile, "hod", hodName);
            } else {
                signaturePath = generateInitials(hodName);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save signature file: " + e.getMessage());
        }

        form.setHodName(hodName);
        form.setHodRemark(hodRemark);
        form.setHodDate(LocalDateTime.now().toLocalDate());
        form.setUpdatedAt(LocalDateTime.now().toLocalDate());
        form.setHodSignaturePath(signaturePath);
        form.setStatus(FormStatus.PENDING_ADMIN);

        return clearanceRepo.save(form);
    }

    // New method to get printable form for corps members
    public Optional<PrintableFormResponseDTO> getPrintableForm(Long formId, String corpsName) {
        Optional<ClearanceForm> formOpt = clearanceRepo.findById(formId);
        if (formOpt.isEmpty()) {
            return Optional.empty();
        }

        ClearanceForm form = formOpt.get();

        // Only allow printing if form is approved and belongs to the requesting corps member
        if (form.getStatus() != FormStatus.APPROVED || !form.getCorpsName().equalsIgnoreCase(corpsName)) {
            return Optional.empty();
        }

        PrintableFormResponseDTO dto = new PrintableFormResponseDTO();
        dto.setFormId(form.getId());
        dto.setCorpsName(form.getCorpsName());
        dto.setStateCode(form.getStateCode());
        dto.setDepartment(form.getDepartment());
        dto.setCreatedAt(form.getCreatedAt());

        // Supervisor information
        dto.setDaysAbsent(form.getDayAbsent());
        dto.setConductRemark(form.getConductRemark());
        dto.setSupervisorName(form.getSupervisorName());
        dto.setSupervisorDate(form.getSupervisorDate());

        // Convert supervisor signature path to URL if it's a file, keep as initials if not
        String supervisorSig = form.getSupervisorSignaturePath();
        if (supervisorSig != null && supervisorSig.contains("_")) { // File format check
            dto.setSupervisorSignatureUrl(signatureService.getSignatureUrl(supervisorSig));
        } else {
            dto.setSupervisorSignatureUrl(supervisorSig); // Keep initials as text
        }

        // HOD information
        dto.setHodRemark(form.getHodRemark());
        dto.setHodName(form.getHodName());
        dto.setHodDate(form.getHodDate());

        // Convert HOD signature path to URL if it's a file, keep as initials if not
        String hodSig = form.getHodSignaturePath();
        if (hodSig != null && hodSig.contains("_")) { // File format check
            dto.setHodSignatureUrl(signatureService.getSignatureUrl(hodSig));
        } else {
            dto.setHodSignatureUrl(hodSig); // Keep initials as text
        }

        // Admin information
        dto.setAdminName(form.getAdminName());
        dto.setApprovalDate(form.getApprovalDate());
        dto.setStatus(form.getStatus());
        dto.setAdminSignatureUrl(signatureService.getSignatureUrl("admin_signature.png"));
        return Optional.of(dto);
    }

    // Keep all existing methods unchanged...
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

//    public long countFormByStatusSinceDate(FormStatus status, LocalDateTime date){
//        return clearanceRepo.countByStatusAndCreatedAtAfter(status,date);
//    }

    public List <ClearanceForm> getPendingFormsForUser(UserRole userRole, String userDepartment){

        switch (userRole){
            case SUPERVISOR :
                return clearanceRepo.findByStatusAndDepartment(FormStatus.PENDING_SUPERVISOR, userDepartment);

            case HOD:
                return clearanceRepo.findByStatusAndDepartment(FormStatus.PENDING_HOD, userDepartment);

            case ADMIN:
                return clearanceRepo.findByStatusAndDepartment(FormStatus.PENDING_ADMIN, userDepartment);

            case CORPS_MEMBER:
                return List.of();

            default:
                throw new IllegalArgumentException("Invalid user role: " + userRole);
        }
    }

    public long getPendingCountForUser(UserRole userRole, String userDepartment){

        switch (userRole){
            case SUPERVISOR :
                return clearanceRepo.countByStatusAndDepartment(FormStatus.PENDING_SUPERVISOR, userDepartment);

            case HOD:
                return clearanceRepo.countByStatusAndDepartment(FormStatus.PENDING_HOD, userDepartment );

            case ADMIN:
                return clearanceRepo.countByStatusAndDepartment(FormStatus.PENDING_ADMIN, userDepartment);

            case CORPS_MEMBER:
                return 0L;

            default:
                throw new IllegalArgumentException("Invalid user role: " + userRole);
        }
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

    public void deleteForm(Long formId, String adminName) {
        Optional<ClearanceForm> formOpt = clearanceRepo.findById(formId);
        if (formOpt.isEmpty()) {
            throw new RuntimeException("Form not found with ID: " + formId);
        }

        ClearanceForm form = formOpt.get();

        // Clean up signature files before deleting
        if (form.getSupervisorSignaturePath() != null && form.getSupervisorSignaturePath().contains("_")) {
            signatureService.deleteSignatureFile(form.getSupervisorSignaturePath());
        }
        if (form.getHodSignaturePath() != null && form.getHodSignaturePath().contains("_")) {
            signatureService.deleteSignatureFile(form.getHodSignaturePath());
        }

        System.out.println("Form with ID " + formId + " (Corps Member: " + form.getCorpsName() +
                ") deleted by admin: " + adminName + " at " + LocalDateTime.now());

        clearanceRepo.deleteById(formId);
    }

    public boolean formExists(Long formId) {
        return clearanceRepo.existsById(formId);
    }
    public byte[] exportFormsToExcel() throws IOException {
        // Fetch data
        List<ClearanceForm> allForms = clearanceRepo.findAll();
        List<Employee> allEmployees = employeeRepository.findAll();
        List<CorpsMember> allCorpsMembers = corpsMemberRepository.findAll();

        Workbook workbook = new XSSFWorkbook();

        // Create sheets
        Sheet formsSheet = workbook.createSheet("Clearance Forms");
        Sheet employeesSheet = workbook.createSheet("Employees");
        Sheet corpsMembersSheet = workbook.createSheet("Corps Members");

        // Header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Populate sheets
        createClearanceFormsSheet(formsSheet, allForms, headerStyle);
        createEmployeesSheet(employeesSheet, allEmployees, headerStyle);
        createCorpsMembersSheet(corpsMembersSheet, allCorpsMembers, headerStyle);

        // Convert to byte[]
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }


    private void createClearanceFormsSheet(Sheet sheet, List<ClearanceForm> allForms, CellStyle headerStyle) {
        // Create headers
        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "ID", "Corps Name", "State Code", "Department", "Status", "Created Date",
                "Days Absent", "Conduct Remark", "Supervisor Name", "Supervisor Date",
                "HOD Name", "HOD Remark", "HOD Date", "Admin Name", "Approval Date", "Approved"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Fill data rows
        int rowNum = 1;
        for (ClearanceForm form : allForms) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(form.getId());
            row.createCell(1).setCellValue(form.getCorpsName());
            row.createCell(2).setCellValue(form.getStateCode());
            row.createCell(3).setCellValue(form.getDepartment());
            row.createCell(4).setCellValue(form.getStatus().toString());
            row.createCell(5).setCellValue(form.getCreatedAt().toString());

            // Supervisor data
            row.createCell(6).setCellValue(form.getDayAbsent() != null ? form.getDayAbsent() : 0);
            row.createCell(7).setCellValue(form.getConductRemark() != null ? form.getConductRemark() : "");
            row.createCell(8).setCellValue(form.getSupervisorName() != null ? form.getSupervisorName() : "");
            row.createCell(9).setCellValue(form.getSupervisorDate() != null ? form.getSupervisorDate().toString() : "");

            // HOD data
            row.createCell(10).setCellValue(form.getHodName() != null ? form.getHodName() : "");
            row.createCell(11).setCellValue(form.getHodRemark() != null ? form.getHodRemark() : "");
            row.createCell(12).setCellValue(form.getHodDate() != null ? form.getHodDate().toString() : "");

            // Admin data
            row.createCell(13).setCellValue(form.getAdminName() != null ? form.getAdminName() : "");
            row.createCell(14).setCellValue(form.getApprovalDate() != null ? form.getApprovalDate().toString() : "");
            row.createCell(15).setCellValue(form.getApproved() != null ? form.getApproved().toString() : "");
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createEmployeesSheet(Sheet sheet, List<Employee> allEmployees, CellStyle headerStyle) {
        // Create headers
        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "ID", "Name", "Department", "Role", "Active", "Created Date", "Last Password Change"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Fill data rows
        int rowNum = 1;
        for (Employee employee : allEmployees) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(employee.getId());
            row.createCell(1).setCellValue(employee.getName());
            row.createCell(2).setCellValue(employee.getDepartment());
            row.createCell(3).setCellValue(employee.getRole().toString());
            row.createCell(4).setCellValue(employee.isActive() ? "YES" : "NO");
            row.createCell(5).setCellValue(employee.getCreatedAt().toString());
            row.createCell(6).setCellValue(employee.getLastPasswordChange().toString());
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createCorpsMembersSheet(Sheet sheet, List<CorpsMember> allCorpsMembers, CellStyle headerStyle) {
        // Create headers
        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "ID", "Name", "Department", "Active", "Created Date"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Fill data rows
        int rowNum = 1;
        for (CorpsMember corpsMember : allCorpsMembers) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(corpsMember.getId());
            row.createCell(1).setCellValue(corpsMember.getName());
            row.createCell(2).setCellValue(corpsMember.getDepartment());
            row.createCell(3).setCellValue(corpsMember.isActive() ? "YES" : "NO");
            row.createCell(4).setCellValue(corpsMember.getCreatedAt().toString());
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}