package com.smarthostel.controller;

import com.smarthostel.dto.EmergencyContactResponse;
import com.smarthostel.exception.BusinessRuleException;
import com.smarthostel.security.AppUserDetails;
import com.smarthostel.service.AllocationService;
import com.smarthostel.service.EmergencyContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/emergency-contacts")
@RequiredArgsConstructor
public class EmergencyContactController {

    private final EmergencyContactService emergencyContactService;
    private final AllocationService allocationService;

    @GetMapping
    public List<EmergencyContactResponse> contacts(@RequestParam(required = false) Long hostelId,
                                                   @AuthenticationPrincipal AppUserDetails user) {
        Long effectiveHostel = hostelId;
        if (effectiveHostel == null && user.isStudent()) {
            effectiveHostel = allocationService.findActiveHostelId(user.getId());
        }
        if (effectiveHostel == null) {
            throw new BusinessRuleException("Choose a hostel to see its contacts");
        }
        return emergencyContactService.getContacts(effectiveHostel);
    }
}
