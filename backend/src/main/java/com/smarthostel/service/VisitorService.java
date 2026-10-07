package com.smarthostel.service;

import com.smarthostel.dto.DecisionRequest;
import com.smarthostel.dto.VisitorApplyRequest;
import com.smarthostel.dto.VisitorResponse;
import com.smarthostel.exception.BusinessRuleException;
import com.smarthostel.exception.ResourceNotFoundException;
import com.smarthostel.model.VisitorPass;
import com.smarthostel.repository.StaffRepository;
import com.smarthostel.repository.VisitorPassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.smarthostel.service.AllocationService.blankToNull;

/**
 * Visitor passes. When a pass is approved, the Oracle trigger trg_visitor_pass_qr
 * generates its QR code; Hibernate reads the generated value back after the update.
 */
@Service
@RequiredArgsConstructor
public class VisitorService {

    private final VisitorPassRepository visitorRepository;
    private final StaffRepository staffRepository;
    private final StudentService studentService;

    @Transactional
    public VisitorResponse apply(Long studentId, VisitorApplyRequest r) {
        VisitorPass v = new VisitorPass();
        v.setStudent(studentService.findStudent(studentId));
        v.setVisitorName(r.visitorName().trim());
        v.setVisitorPhone(r.visitorPhone());
        v.setRelationToStudent(r.relationToStudent());
        v.setPurpose(r.purpose());
        v.setVisitDate(r.visitDate());
        v.setStatus("Pending");
        return VisitorResponse.from(visitorRepository.saveAndFlush(v));
    }

    @Transactional(readOnly = true)
    public List<VisitorResponse> ofStudent(Long studentId) {
        return visitorRepository.findByStudent(studentId).stream().map(VisitorResponse::from).toList();
    }

    @Transactional
    public void cancel(Long passId, Long studentId) {
        VisitorPass v = findPass(passId);
        if (!v.getStudent().getId().equals(studentId)) {
            throw new AccessDeniedException("Not your visitor pass");
        }
        if (!"Pending".equals(v.getStatus())) {
            throw new BusinessRuleException("Only pending requests can be withdrawn");
        }
        visitorRepository.delete(v);
    }

    @Transactional(readOnly = true)
    public List<VisitorResponse> search(String status, LocalDate visitDate, String q) {
        return visitorRepository.search(blankToNull(status), visitDate, blankToNull(q)).stream()
                .map(VisitorResponse::from).toList();
    }

    @Transactional
    public VisitorResponse decide(Long passId, DecisionRequest request, Long staffId) {
        VisitorPass v = findPass(passId);
        if (!"Pending".equals(v.getStatus())) {
            throw new BusinessRuleException("Visitor pass is already " + v.getStatus());
        }
        v.setStatus(request.decision());
        v.setApprovedBy(staffRepository.getReferenceById(staffId));
        visitorRepository.saveAndFlush(v);       // trigger generates qr_code on approval
        return VisitorResponse.from(v);
    }

    /**
     * Gate scan: the first scan records entry, the second records exit and completes the pass.
     */
    @Transactional
    public VisitorResponse scan(String qrCode) {
        VisitorPass v = visitorRepository.findByQrCode(qrCode.trim())
                .orElseThrow(() -> new BusinessRuleException("Unknown QR code"));
        if (!"Approved".equals(v.getStatus())) {
            throw new BusinessRuleException("Pass is " + v.getStatus() + " and cannot be used");
        }
        if (!v.getVisitDate().equals(LocalDate.now())) {
            throw new BusinessRuleException("Pass is valid only on " + v.getVisitDate());
        }
        if (v.getEntryTime() == null) {
            v.setEntryTime(LocalDateTime.now());
        } else {
            v.setExitTime(LocalDateTime.now());
            v.setStatus("Completed");
        }
        visitorRepository.saveAndFlush(v);
        return VisitorResponse.from(v);
    }

    @Transactional(readOnly = true)
    public VisitorResponse getPass(Long id, Long studentIdOrNull) {
        VisitorPass v = findPass(id);
        if (studentIdOrNull != null && !v.getStudent().getId().equals(studentIdOrNull)) {
            throw new AccessDeniedException("Not your visitor pass");
        }
        return VisitorResponse.from(v);
    }

    private VisitorPass findPass(Long id) {
        return visitorRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Visitor pass", id));
    }
}
