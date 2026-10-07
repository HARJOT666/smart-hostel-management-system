package com.smarthostel.service;

import com.smarthostel.dto.DecisionRequest;
import com.smarthostel.dto.LeaveApplyRequest;
import com.smarthostel.dto.LeaveResponse;
import com.smarthostel.exception.BusinessRuleException;
import com.smarthostel.exception.ResourceNotFoundException;
import com.smarthostel.model.LeaveRequest;
import com.smarthostel.repository.LeaveRequestRepository;
import com.smarthostel.repository.PlsqlRepository;
import com.smarthostel.security.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static com.smarthostel.service.AllocationService.blankToNull;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRepository;
    private final PlsqlRepository plsqlRepository;
    private final StudentService studentService;

    @Transactional
    public LeaveResponse apply(Long studentId, LeaveApplyRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessRuleException("End date cannot be before start date");
        }
        LeaveRequest l = new LeaveRequest();
        l.setStudent(studentService.findStudent(studentId));
        l.setStartDate(request.startDate());
        l.setEndDate(request.endDate());
        l.setReason(request.reason().trim());
        l.setDestination(request.destination());
        l.setAppliedDate(LocalDate.now());
        l.setStatus("Pending");
        return LeaveResponse.from(leaveRepository.saveAndFlush(l));
    }

    @Transactional(readOnly = true)
    public List<LeaveResponse> ofStudent(Long studentId) {
        return leaveRepository.findByStudent(studentId).stream().map(LeaveResponse::from).toList();
    }

    /** A student may withdraw (DELETE) their own leave request while it is still pending. */
    @Transactional
    public void cancel(Long leaveId, Long studentId) {
        LeaveRequest l = findLeave(leaveId);
        if (!l.getStudent().getId().equals(studentId)) {
            throw new AccessDeniedException("Not your leave request");
        }
        if (!"Pending".equals(l.getStatus())) {
            throw new BusinessRuleException("Only pending requests can be withdrawn");
        }
        leaveRepository.delete(l);
    }

    @Transactional(readOnly = true)
    public List<LeaveResponse> search(String status, String q) {
        return leaveRepository.search(blankToNull(status), blankToNull(q)).stream().map(LeaveResponse::from).toList();
    }

    /** Calls sp_decide_leave, which allows only Pending -> Approved/Rejected by a warden/admin. */
    @Transactional
    public LeaveResponse decide(Long leaveId, DecisionRequest request, Long staffId) {
        plsqlRepository.decideLeave(leaveId, staffId, request.decision());
        return LeaveResponse.from(findLeave(leaveId));
    }

    @Transactional(readOnly = true)
    public LeaveResponse getLeave(Long id, AppUserDetails user) {
        LeaveRequest l = findLeave(id);
        if (user.isStudent() && !l.getStudent().getId().equals(user.getId())) {
            throw new AccessDeniedException("Not your leave request");
        }
        return LeaveResponse.from(l);
    }

    private LeaveRequest findLeave(Long id) {
        return leaveRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Leave request", id));
    }
}
