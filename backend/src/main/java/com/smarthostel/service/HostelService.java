package com.smarthostel.service;

import com.smarthostel.config.CacheNames;
import com.smarthostel.dto.HostelRequest;
import com.smarthostel.dto.HostelResponse;
import com.smarthostel.exception.ResourceNotFoundException;
import com.smarthostel.model.Hostel;
import com.smarthostel.repository.HostelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HostelService {

    private final HostelRepository hostelRepository;

    /**
     * Cached in Redis (key shms:hostels::all, TTL 60 min).
     * The log line below only appears when the data really comes from Oracle (cache miss).
     */
    @Cacheable(value = CacheNames.HOSTELS, key = "'all'")
    @Transactional(readOnly = true)
    public List<HostelResponse> getAllHostels() {
        log.info("CACHE MISS: loading hostels from Oracle");
        return hostelRepository.findAllByOrderByIdAsc().stream().map(HostelResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public HostelResponse getHostel(Long id) {
        return HostelResponse.from(findHostel(id));
    }

    /** Any change to hostels removes the cached list, so the next read is fresh. */
    @CacheEvict(value = CacheNames.HOSTELS, allEntries = true)
    @Transactional
    public HostelResponse createHostel(HostelRequest request) {
        Hostel hostel = new Hostel();
        copy(request, hostel);
        return HostelResponse.from(hostelRepository.save(hostel));
    }

    @CacheEvict(value = CacheNames.HOSTELS, allEntries = true)
    @Transactional
    public HostelResponse updateHostel(Long id, HostelRequest request) {
        Hostel hostel = findHostel(id);
        copy(request, hostel);
        return HostelResponse.from(hostelRepository.save(hostel));
    }

    Hostel findHostel(Long id) {
        return hostelRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Hostel", id));
    }

    private void copy(HostelRequest r, Hostel h) {
        h.setHostelName(r.hostelName().trim());
        h.setGenderType(r.genderType());
        h.setLocation(r.location().trim());
        h.setTotalFloors(r.totalFloors());
    }
}
