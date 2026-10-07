package com.smarthostel.controller;

import com.smarthostel.dto.MaintenanceOption;
import com.smarthostel.dto.StaffRequest;
import com.smarthostel.dto.StaffResponse;
import com.smarthostel.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
public class StaffController {

    private final StaffService staffService;

    @GetMapping
    public List<StaffResponse> search(@RequestParam(required = false) String role,
                                      @RequestParam(required = false) Long hostelId) {
        return staffService.search(role, hostelId);
    }

    /** Eligible maintenance staff for a complaint, with their current workload. */
    @GetMapping("/maintenance-options")
    public List<MaintenanceOption> maintenanceOptions(@RequestParam Long complaintId) {
        return staffService.maintenanceOptionsFor(complaintId);
    }

    @GetMapping("/{id}")
    public StaffResponse getOne(@PathVariable Long id) {
        return staffService.getStaff(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public StaffResponse create(@Valid @RequestBody StaffRequest request) {
        return staffService.createStaff(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public StaffResponse update(@PathVariable Long id, @Valid @RequestBody StaffRequest request) {
        return staffService.updateStaff(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        staffService.deleteStaff(id);
    }
}
