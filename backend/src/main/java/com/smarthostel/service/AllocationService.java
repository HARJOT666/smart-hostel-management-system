package com.smarthostel.service;

import com.smarthostel.config.CacheNames;
import com.smarthostel.dto.AllocateRequest;
import com.smarthostel.dto.AllocationResponse;
import com.smarthostel.exception.ResourceNotFoundException;
import com.smarthostel.model.RoomAllocation;
import com.smarthostel.repository.PlsqlRepository;
import com.smarthostel.repository.RoomAllocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Room allocation. The rules (student exists, no active allocation, gender, capacity,
 * free bed) are all checked inside the Oracle procedure sp_allocate_room, and the
 * trigger trg_allocation_occupancy updates the room's occupancy.
 * Allocations are never cached.
 */
@Service
@RequiredArgsConstructor
public class AllocationService {

    private final RoomAllocationRepository allocationRepository;
    private final PlsqlRepository plsqlRepository;

    @Transactional(readOnly = true)
    public List<AllocationResponse> search(String status, Long hostelId, String q) {
        return allocationRepository.search(blankToNull(status), hostelId, blankToNull(q)).stream()
                .map(AllocationResponse::from).toList();
    }

    /** One transaction: call the procedure, then read the new row. Any ORA-20xxx error rolls it all back. */
    @CacheEvict(value = CacheNames.DASHBOARD_STATS, allEntries = true)
    @Transactional
    public AllocationResponse allocate(AllocateRequest request, Long staffId) {
        PlsqlRepository.AllocationResult result =
                plsqlRepository.allocateRoom(request.studentId(), request.roomId(), staffId);
        return AllocationResponse.from(findWithDetails(result.allocationId()));
    }

    @CacheEvict(value = CacheNames.DASHBOARD_STATS, allEntries = true)
    @Transactional
    public AllocationResponse vacate(Long allocationId) {
        plsqlRepository.vacateRoom(allocationId);
        return AllocationResponse.from(findWithDetails(allocationId));
    }

    @Transactional(readOnly = true)
    public List<AllocationResponse> historyOfStudent(Long studentId) {
        return allocationRepository.findHistoryByStudentId(studentId).stream().map(AllocationResponse::from).toList();
    }

    /** Hostel id of the student's current room, or null if the student has no room. */
    @Transactional(readOnly = true)
    public Long findActiveHostelId(Long studentId) {
        return allocationRepository.findActiveByStudentId(studentId)
                .map(a -> a.getRoom().getHostel().getId())
                .orElse(null);
    }

    private RoomAllocation findWithDetails(Long id) {
        return allocationRepository.findWithDetails(id).orElseThrow(() -> new ResourceNotFoundException("Allocation", id));
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
