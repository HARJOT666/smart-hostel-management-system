package com.smarthostel.controller;

import com.smarthostel.dto.AnnouncementRequest;
import com.smarthostel.dto.AnnouncementResponse;
import com.smarthostel.security.AppUserDetails;
import com.smarthostel.service.AllocationService;
import com.smarthostel.service.AnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final AllocationService allocationService;

    /** Students automatically get their own hostel's notices; staff can choose a hostel (or all). */
    @GetMapping
    public List<AnnouncementResponse> list(@RequestParam(required = false) Long hostelId,
                                           @AuthenticationPrincipal AppUserDetails user) {
        Long effectiveHostel = user.isStudent() ? allocationService.findActiveHostelId(user.getId()) : hostelId;
        if (user.isStudent() && effectiveHostel == null) {
            effectiveHostel = 0L;   // no room yet -> only global notices
        }
        return announcementService.getAnnouncements(effectiveHostel);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public AnnouncementResponse create(@Valid @RequestBody AnnouncementRequest request,
                                       @AuthenticationPrincipal AppUserDetails user) {
        return announcementService.create(request, user.getId());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public AnnouncementResponse update(@PathVariable Long id, @Valid @RequestBody AnnouncementRequest request) {
        return announcementService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public void delete(@PathVariable Long id) {
        announcementService.delete(id);
    }
}
