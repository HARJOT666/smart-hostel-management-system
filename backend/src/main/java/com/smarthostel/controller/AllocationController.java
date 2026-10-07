package com.smarthostel.controller;

import com.smarthostel.dto.AllocateRequest;
import com.smarthostel.dto.AllocationResponse;
import com.smarthostel.security.AppUserDetails;
import com.smarthostel.service.AllocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/allocations")
@RequiredArgsConstructor
public class AllocationController {

    private final AllocationService allocationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public List<AllocationResponse> search(@RequestParam(required = false) String status,
                                           @RequestParam(required = false) Long hostelId,
                                           @RequestParam(required = false) String q) {
        return allocationService.search(status, hostelId, q);
    }

    /** Allocates a room by calling the Oracle procedure sp_allocate_room. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public AllocationResponse allocate(@Valid @RequestBody AllocateRequest request,
                                       @AuthenticationPrincipal AppUserDetails user) {
        return allocationService.allocate(request, user.getId());
    }

    /** Vacates a room by calling the Oracle procedure sp_vacate_room. */
    @PutMapping("/{id}/vacate")
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public AllocationResponse vacate(@PathVariable Long id) {
        return allocationService.vacate(id);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public List<AllocationResponse> myHistory(@AuthenticationPrincipal AppUserDetails user) {
        return allocationService.historyOfStudent(user.getId());
    }
}
