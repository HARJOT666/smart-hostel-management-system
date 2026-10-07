package com.smarthostel.controller;

import com.smarthostel.dto.*;
import com.smarthostel.repository.PlsqlRepository;
import com.smarthostel.security.AppUserDetails;
import com.smarthostel.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    // ---------- student ----------

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    public ComplaintResponse raise(@Valid @RequestBody ComplaintRequest request,
                                   @AuthenticationPrincipal AppUserDetails user) {
        return complaintService.raise(user.getId(), request);
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ComplaintResponse uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file,
                                         @AuthenticationPrincipal AppUserDetails user) {
        return complaintService.attachImage(id, file, user);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public List<ComplaintResponse> mine(@AuthenticationPrincipal AppUserDetails user) {
        return complaintService.ofStudent(user.getId());
    }

    // ---------- admin / warden ----------

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public List<ComplaintResponse> search(@RequestParam(required = false) String status,
                                          @RequestParam(required = false) String category,
                                          @RequestParam(required = false) String q,
                                          @RequestParam(required = false) Long hostelId) {
        return complaintService.search(status, category, q, hostelId);
    }

    /** Calls sp_assign_complaint. */
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public ComplaintResponse assign(@PathVariable Long id, @Valid @RequestBody AssignComplaintRequest request) {
        return complaintService.assign(id, request);
    }

    /** Calls the cursor procedure sp_auto_assign_open_complaints. */
    @PostMapping("/auto-assign")
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public PlsqlRepository.AutoAssignResult autoAssign() {
        return complaintService.autoAssign();
    }

    // ---------- maintenance ----------

    @GetMapping("/assigned")
    @PreAuthorize("hasRole('MAINTENANCE')")
    public List<ComplaintResponse> assignedToMe(@RequestParam(required = false) String status,
                                                @AuthenticationPrincipal AppUserDetails user) {
        return complaintService.assignedTo(user.getId(), status);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('MAINTENANCE')")
    public ComplaintResponse updateStatus(@PathVariable Long id, @Valid @RequestBody ComplaintStatusRequest request,
                                          @AuthenticationPrincipal AppUserDetails user) {
        return complaintService.updateStatus(id, request, user);
    }

    @PostMapping(value = "/{id}/completion-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MAINTENANCE')")
    public ComplaintResponse uploadCompletionImage(@PathVariable Long id, @RequestParam("file") MultipartFile file,
                                                   @AuthenticationPrincipal AppUserDetails user) {
        return complaintService.attachCompletionImage(id, file, user);
    }

    // ---------- any role (access checked in the service) ----------

    @GetMapping("/{id}")
    public ComplaintResponse getOne(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails user) {
        return complaintService.getComplaint(id, user);
    }
}
