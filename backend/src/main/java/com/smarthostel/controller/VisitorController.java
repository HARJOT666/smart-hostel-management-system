package com.smarthostel.controller;

import com.smarthostel.dto.DecisionRequest;
import com.smarthostel.dto.ScanRequest;
import com.smarthostel.dto.VisitorApplyRequest;
import com.smarthostel.dto.VisitorResponse;
import com.smarthostel.security.AppUserDetails;
import com.smarthostel.service.VisitorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/visitors")
@RequiredArgsConstructor
public class VisitorController {

    private final VisitorService visitorService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    public VisitorResponse apply(@Valid @RequestBody VisitorApplyRequest request,
                                 @AuthenticationPrincipal AppUserDetails user) {
        return visitorService.apply(user.getId(), request);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public List<VisitorResponse> mine(@AuthenticationPrincipal AppUserDetails user) {
        return visitorService.ofStudent(user.getId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('STUDENT')")
    public void cancel(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails user) {
        visitorService.cancel(id, user.getId());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public List<VisitorResponse> search(@RequestParam(required = false) String status,
                                        @RequestParam(required = false)
                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate visitDate,
                                        @RequestParam(required = false) String q) {
        return visitorService.search(status, visitDate, q);
    }

    /** Approve/reject. On approval the Oracle trigger trg_visitor_pass_qr creates the QR code. */
    @PutMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public VisitorResponse decide(@PathVariable Long id, @Valid @RequestBody DecisionRequest request,
                                  @AuthenticationPrincipal AppUserDetails user) {
        return visitorService.decide(id, request, user.getId());
    }

    /** Gate check-in / check-out using the pass QR code. */
    @PostMapping("/scan")
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public VisitorResponse scan(@Valid @RequestBody ScanRequest request) {
        return visitorService.scan(request.qrCode());
    }

    @GetMapping("/{id}")
    public VisitorResponse getOne(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails user) {
        return visitorService.getPass(id, user.isStudent() ? user.getId() : null);
    }
}
