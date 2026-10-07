package com.smarthostel.controller;

import com.smarthostel.dto.MessMenuResponse;
import com.smarthostel.dto.MessMenuUpdateRequest;
import com.smarthostel.exception.BusinessRuleException;
import com.smarthostel.security.AppUserDetails;
import com.smarthostel.service.AllocationService;
import com.smarthostel.service.MessMenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mess-menu")
@RequiredArgsConstructor
public class MessMenuController {

    private final MessMenuService messMenuService;
    private final AllocationService allocationService;

    @GetMapping
    public List<MessMenuResponse> menu(@RequestParam(required = false) Long hostelId,
                                       @AuthenticationPrincipal AppUserDetails user) {
        Long effectiveHostel = hostelId;
        if (effectiveHostel == null && user.isStudent()) {
            effectiveHostel = allocationService.findActiveHostelId(user.getId());
        }
        if (effectiveHostel == null) {
            throw new BusinessRuleException("Choose a hostel to see its mess menu");
        }
        return messMenuService.getMenu(effectiveHostel);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public MessMenuResponse update(@PathVariable Long id, @Valid @RequestBody MessMenuUpdateRequest request) {
        return messMenuService.updateMeal(id, request);
    }
}
