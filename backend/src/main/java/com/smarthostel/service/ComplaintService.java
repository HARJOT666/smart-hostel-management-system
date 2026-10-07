package com.smarthostel.service;

import com.smarthostel.dto.*;
import com.smarthostel.exception.BusinessRuleException;
import com.smarthostel.exception.ResourceNotFoundException;
import com.smarthostel.model.Complaint;
import com.smarthostel.model.RoomAllocation;
import com.smarthostel.repository.ComplaintRepository;
import com.smarthostel.repository.PlsqlRepository;
import com.smarthostel.repository.RoomAllocationRepository;
import com.smarthostel.security.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.smarthostel.service.AllocationService.blankToNull;

/**
 * Complaint workflow:
 * student raises (Open) -> warden assigns via sp_assign_complaint (Assigned)
 * -> maintenance starts (In Progress) -> maintenance resolves (Resolved; the trigger
 * trg_complaint_status stamps resolved_date). Complaint status is never cached.
 */
@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final RoomAllocationRepository allocationRepository;
    private final PlsqlRepository plsqlRepository;
    private final StudentService studentService;
    private final FileStorageService fileStorageService;

    // ---------------- student ----------------

    @Transactional
    public ComplaintResponse raise(Long studentId, ComplaintRequest request) {
        Complaint c = new Complaint();
        c.setStudent(studentService.findStudent(studentId));
        c.setCategory(request.category());
        c.setDescription(request.description().trim());
        c.setPriority(request.priority() == null ? "Medium" : request.priority());
        c.setStatus("Open");
        c.setCreatedDate(LocalDateTime.now());
        complaintRepository.saveAndFlush(c);
        return toResponse(c);
    }

    @Transactional
    public ComplaintResponse attachImage(Long complaintId, MultipartFile file, AppUserDetails user) {
        Complaint c = findComplaint(complaintId);
        if (!c.getStudent().getId().equals(user.getId())) {
            throw new AccessDeniedException("Not your complaint");
        }
        c.setImagePath(fileStorageService.storeImage(file, "complaints"));
        complaintRepository.saveAndFlush(c);
        return toResponse(c);
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> ofStudent(Long studentId) {
        return toResponses(complaintRepository.findByStudent(studentId));
    }

    // ---------------- admin / warden ----------------

    @Transactional(readOnly = true)
    public List<ComplaintResponse> search(String status, String category, String q, Long hostelId) {
        return toResponses(complaintRepository.search(blankToNull(status), blankToNull(category), blankToNull(q))).stream()
                .filter(c -> hostelId == null || hostelNameMatches(c, hostelId))
                .toList();
    }

    /** Calls the Oracle procedure sp_assign_complaint (checks role, specialization and hostel). */
    @Transactional
    public ComplaintResponse assign(Long complaintId, AssignComplaintRequest request) {
        plsqlRepository.assignComplaint(complaintId, request.staffId(), request.scheduledDate());
        return getComplaint(complaintId);
    }

    /** Calls the cursor procedure sp_auto_assign_open_complaints. */
    @Transactional
    public PlsqlRepository.AutoAssignResult autoAssign() {
        return plsqlRepository.autoAssignOpenComplaints();
    }

    // ---------------- maintenance ----------------

    @Transactional(readOnly = true)
    public List<ComplaintResponse> assignedTo(Long staffId, String status) {
        return toResponses(complaintRepository.findByAssignedStaff(staffId, blankToNull(status)));
    }

    @Transactional
    public ComplaintResponse updateStatus(Long complaintId, ComplaintStatusRequest request, AppUserDetails user) {
        Complaint c = findComplaint(complaintId);
        if (c.getAssignedTo() == null || !c.getAssignedTo().getId().equals(user.getId())) {
            throw new AccessDeniedException("This complaint is not assigned to you");
        }
        if ("Resolved".equals(c.getStatus())) {
            throw new BusinessRuleException("Complaint is already resolved");
        }
        c.setStatus(request.status());
        if (request.remarks() != null && !request.remarks().isBlank()) {
            c.setRemarks(request.remarks().trim());
        }
        if (request.scheduledDate() != null) {
            c.setScheduledDate(request.scheduledDate());
        }
        // saveAndFlush runs the UPDATE now; the trigger sets resolved_date and Hibernate reads it back
        complaintRepository.saveAndFlush(c);
        return toResponse(c);
    }

    @Transactional
    public ComplaintResponse attachCompletionImage(Long complaintId, MultipartFile file, AppUserDetails user) {
        Complaint c = findComplaint(complaintId);
        if (c.getAssignedTo() == null || !c.getAssignedTo().getId().equals(user.getId())) {
            throw new AccessDeniedException("This complaint is not assigned to you");
        }
        c.setCompletionImagePath(fileStorageService.storeImage(file, "completed"));
        complaintRepository.saveAndFlush(c);
        return toResponse(c);
    }

    // ---------------- shared ----------------

    /** Students see only their own complaints, maintenance only those assigned to them. */
    @Transactional(readOnly = true)
    public ComplaintResponse getComplaint(Long id, AppUserDetails user) {
        Complaint c = findComplaint(id);
        boolean allowed = switch (user.getRole()) {
            case "STUDENT" -> c.getStudent().getId().equals(user.getId());
            case "MAINTENANCE" -> c.getAssignedTo() != null && c.getAssignedTo().getId().equals(user.getId());
            default -> true;
        };
        if (!allowed) {
            throw new AccessDeniedException("Not allowed");
        }
        return toResponse(c);
    }

    @Transactional(readOnly = true)
    public ComplaintResponse getComplaint(Long id) {
        return toResponse(findComplaint(id));
    }

    private Complaint findComplaint(Long id) {
        return complaintRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Complaint", id));
    }

    private ComplaintResponse toResponse(Complaint c) {
        RoomAllocation room = allocationRepository.findActiveByStudentId(c.getStudent().getId()).orElse(null);
        return ComplaintResponse.from(c, room);
    }

    /** Looks up every student's room with ONE query instead of one query per complaint. */
    private List<ComplaintResponse> toResponses(List<Complaint> complaints) {
        if (complaints.isEmpty()) return List.of();
        List<Long> studentIds = complaints.stream().map(c -> c.getStudent().getId()).distinct().toList();
        Map<Long, RoomAllocation> rooms = allocationRepository.findActiveByStudentIds(studentIds).stream()
                .collect(Collectors.toMap(a -> a.getStudent().getId(), Function.identity()));
        return complaints.stream().map(c -> ComplaintResponse.from(c, rooms.get(c.getStudent().getId()))).toList();
    }

    private boolean hostelNameMatches(ComplaintResponse c, Long hostelId) {
        return allocationRepository.findActiveByStudentId(c.studentId())
                .map(a -> a.getRoom().getHostel().getId().equals(hostelId))
                .orElse(false);
    }
}
