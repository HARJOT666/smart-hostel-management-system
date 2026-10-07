package com.smarthostel.controller;

import com.smarthostel.dto.FeeResponse;
import com.smarthostel.dto.PaymentRequest;
import com.smarthostel.security.AppUserDetails;
import com.smarthostel.service.FeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fees")
@RequiredArgsConstructor
public class FeeController {

    private final FeeService feeService;

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public List<FeeResponse> mine(@AuthenticationPrincipal AppUserDetails user) {
        return feeService.ofStudent(user.getId());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public List<FeeResponse> search(@RequestParam(required = false) String semester,
                                    @RequestParam(defaultValue = "false") boolean outstandingOnly,
                                    @RequestParam(required = false) String q) {
        return feeService.search(semester, outstandingOnly, q);
    }

    @PutMapping("/{id}/payment")
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public FeeResponse recordPayment(@PathVariable Long id, @Valid @RequestBody PaymentRequest request) {
        return feeService.recordPayment(id, request);
    }
}
