package com.smarthostel.service;

import com.smarthostel.config.CacheNames;
import com.smarthostel.dto.EmergencyContactResponse;
import com.smarthostel.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Emergency contacts = wardens and maintenance staff of a hostel (from the STAFF table). */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmergencyContactService {

    private final StaffRepository staffRepository;

    /** Cached per hostel (key shms:emergencyContacts::<hostelId>, TTL 60 min). */
    @Cacheable(value = CacheNames.EMERGENCY_CONTACTS, key = "#hostelId")
    @Transactional(readOnly = true)
    public List<EmergencyContactResponse> getContacts(Long hostelId) {
        log.info("CACHE MISS: loading emergency contacts of hostel {} from Oracle", hostelId);
        return staffRepository.findContactsForHostel(hostelId).stream().map(EmergencyContactResponse::from).toList();
    }
}
