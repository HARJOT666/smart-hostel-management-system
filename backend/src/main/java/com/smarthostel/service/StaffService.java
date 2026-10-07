package com.smarthostel.service;

import com.smarthostel.config.CacheNames;
import com.smarthostel.dto.MaintenanceOption;
import com.smarthostel.dto.StaffRequest;
import com.smarthostel.dto.StaffResponse;
import com.smarthostel.exception.BusinessRuleException;
import com.smarthostel.exception.ResourceNotFoundException;
import com.smarthostel.model.Complaint;
import com.smarthostel.model.Staff;
import com.smarthostel.repository.ComplaintRepository;
import com.smarthostel.repository.RoomAllocationRepository;
import com.smarthostel.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.smarthostel.service.AllocationService.blankToNull;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository staffRepository;
    private final ComplaintRepository complaintRepository;
    private final RoomAllocationRepository allocationRepository;
    private final HostelService hostelService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<StaffResponse> search(String role, Long hostelId) {
        return staffRepository.search(blankToNull(role), hostelId).stream().map(StaffResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public StaffResponse getStaff(Long id) {
        return StaffResponse.from(findStaff(id));
    }

    /** Staff who can take this complaint (same hostel, matching specialization), least busy first. */
    @Transactional(readOnly = true)
    public List<MaintenanceOption> maintenanceOptionsFor(Long complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", complaintId));
        Long hostelId = allocationRepository.findActiveByStudentId(complaint.getStudent().getId())
                .map(a -> a.getRoom().getHostel().getId())
                .orElse(null);
        return staffRepository.findMaintenanceOptions(hostelId, complaint.getCategory());
    }

    // Staff changes can change the emergency contact list, so that cache is cleared.

    @CacheEvict(value = CacheNames.EMERGENCY_CONTACTS, allEntries = true)
    @Transactional
    public StaffResponse createStaff(StaffRequest request) {
        if (request.password() == null || request.password().length() < 8) {
            throw new BusinessRuleException("An initial password of at least 8 characters is required");
        }
        if (staffRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessRuleException("E-mail " + request.email() + " is already in use");
        }
        Staff staff = new Staff();
        copy(request, staff);
        staff.setPasswordHash(passwordEncoder.encode(request.password()));
        return StaffResponse.from(staffRepository.saveAndFlush(staff));
    }

    @CacheEvict(value = CacheNames.EMERGENCY_CONTACTS, allEntries = true)
    @Transactional
    public StaffResponse updateStaff(Long id, StaffRequest request) {
        Staff staff = findStaff(id);
        copy(request, staff);
        if (request.password() != null && !request.password().isBlank()) {
            staff.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return StaffResponse.from(staffRepository.saveAndFlush(staff));
    }

    @CacheEvict(value = CacheNames.EMERGENCY_CONTACTS, allEntries = true)
    @Transactional
    public void deleteStaff(Long id) {
        staffRepository.delete(findStaff(id));
        staffRepository.flush();
    }

    Staff findStaff(Long id) {
        return staffRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Staff", id));
    }

    private void copy(StaffRequest r, Staff s) {
        boolean maintenance = "Maintenance".equals(r.role());
        if (maintenance && r.specialization() == null) {
            throw new BusinessRuleException("Maintenance staff need a specialization");
        }
        s.setName(r.name().trim());
        s.setRole(r.role());
        s.setSpecialization(maintenance ? r.specialization() : null);
        s.setPhone(r.phone());
        s.setEmail(r.email().trim().toLowerCase());
        s.setHostel(hostelService.findHostel(r.hostelId()));
    }
}
