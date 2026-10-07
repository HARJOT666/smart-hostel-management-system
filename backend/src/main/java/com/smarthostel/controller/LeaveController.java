package com.smarthostel.controller;

import com.smarthostel.dto.DecisionRequest;
import com.smarthostel.dto.LeaveApplyRequest;
import com.smarthostel.dto.LeaveResponse;
import com.smarthostel.security.AppUserDetails;
import com.smarthostel.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    public LeaveResponse apply(@Valid @RequestBody LeaveApplyRequest request,
                               @AuthenticationPrincipal AppUserDetails user) {
        return leaveService.apply(user.getId(), request);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public List<LeaveResponse> mine(@AuthenticationPrincipal AppUserDetails user) {
        return leaveService.ofStudent(user.getId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('STUDENT')")
    public void cancel(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails user) {
        leaveService.cancel(id, user.getId());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public List<LeaveResponse> search(@RequestParam(required = false) String status,
                                      @RequestParam(required = false) String q) {
        return leaveService.search(status, q);
    }

    /** Calls the Oracle procedure sp_decide_leave. */
    @PutMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public LeaveResponse decide(@PathVariable Long id, @Valid @RequestBody DecisionRequest request,
                                @AuthenticationPrincipal AppUserDetails user) {
        return leaveService.decide(id, request, user.getId());
    }

    /** Used for the printable approval letter. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','WARDEN')")
    public LeaveResponse getOne(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails user) {
        return leaveService.getLeave(id, user);
    }
}
