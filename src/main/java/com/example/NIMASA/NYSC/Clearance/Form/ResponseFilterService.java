package com.example.NIMASA.NYSC.Clearance.Form;



import com.example.NIMASA.NYSC.Clearance.Form.DTOs.FilteredClearanceFormResponseDTO;
import com.example.NIMASA.NYSC.Clearance.Form.UserRole;
import com.example.NIMASA.NYSC.Clearance.Form.model.ClearanceForm;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResponseFilterService {

    public FilteredClearanceFormResponseDTO filterFormByRole(ClearanceForm form, UserRole userRole) {
        FilteredClearanceFormResponseDTO dto = new FilteredClearanceFormResponseDTO();

        // Basic fields visible to all roles
        dto.setId(form.getId());
        dto.setCorpsName(form.getCorpsName());
        dto.setStateCode(form.getStateCode());
        dto.setDepartment(form.getDepartment());
        dto.setStatus(form.getStatus());
        dto.setCreatedAt(form.getCreatedAt());
        dto.setUpdatedAt(form.getUpdatedAt());

        // Supervisor fields - visible to SUPERVISOR, HOD, ADMIN
        if (userRole == UserRole.SUPERVISOR || userRole == UserRole.ADMIN) {
            dto.setDayAbsent(form.getDayAbsent());
            dto.setConductRemark(form.getConductRemark());
            dto.setSupervisorName(form.getSupervisorName());
            dto.setSupervisorSignaturePath(form.getSupervisorSignaturePath());
            dto.setSupervisorDate(form.getSupervisorDate());
        }

        // HOD fields - visible to HOD, ADMIN
        if (userRole == UserRole.HOD || userRole == UserRole.ADMIN) {
            dto.setHodRemark(form.getHodRemark());
            dto.setHodName(form.getHodName());
            dto.setHodSignaturePath(form.getHodSignaturePath());
            dto.setHodDate(form.getHodDate());
        }

        // Admin fields - visible to ADMIN only
        if (userRole == UserRole.ADMIN) {
            dto.setAdminName(form.getAdminName());
            dto.setApprovalDate(form.getApprovalDate());
            dto.setApproved(form.getApproved());
        }

        return dto;
    }

    public List<FilteredClearanceFormResponseDTO> filterFormsByRole(List<ClearanceForm> forms, UserRole userRole) {
        return forms.stream()
                .map(form -> filterFormByRole(form, userRole))
                .collect(Collectors.toList());
    }
}
