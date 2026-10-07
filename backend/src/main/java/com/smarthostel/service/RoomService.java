package com.smarthostel.service;

import com.smarthostel.config.CacheNames;
import com.smarthostel.dto.AllocationResponse;
import com.smarthostel.dto.RoomRequest;
import com.smarthostel.dto.RoomResponse;
import com.smarthostel.exception.BusinessRuleException;
import com.smarthostel.exception.ResourceNotFoundException;
import com.smarthostel.model.Room;
import com.smarthostel.repository.RoomAllocationRepository;
import com.smarthostel.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Rooms are NOT cached: their occupancy changes with every allocation. */
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomAllocationRepository allocationRepository;
    private final HostelService hostelService;

    @Transactional(readOnly = true)
    public List<RoomResponse> searchRooms(Long hostelId, String roomType, boolean availableOnly) {
        return roomRepository.search(hostelId, roomType).stream()
                .filter(r -> !availableOnly || r.getAvailableBeds() > 0)
                .map(RoomResponse::from)
                .toList();
    }

    /** Rooms with free beds for a student of the given gender (uses fn_available_beds in Oracle). */
    @Transactional(readOnly = true)
    public List<RoomResponse> findAvailableRooms(String gender, Long hostelId) {
        return roomRepository.findAvailableRooms(gender, hostelId).stream().map(RoomResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoom(Long id) {
        return RoomResponse.from(findRoom(id));
    }

    @Transactional(readOnly = true)
    public List<AllocationResponse> getOccupants(Long roomId) {
        return allocationRepository.findActiveByRoomId(roomId).stream().map(AllocationResponse::from).toList();
    }

    @CacheEvict(value = CacheNames.DASHBOARD_STATS, allEntries = true)
    @Transactional
    public RoomResponse createRoom(RoomRequest request) {
        if (roomRepository.existsByHostelIdAndRoomNumberIgnoreCase(request.hostelId(), request.roomNumber())) {
            throw new BusinessRuleException("Room " + request.roomNumber() + " already exists in this hostel");
        }
        Room room = new Room();
        room.setHostel(hostelService.findHostel(request.hostelId()));
        copy(request, room);
        return RoomResponse.from(roomRepository.saveAndFlush(room));
    }

    /** Changing the type of an occupied room to a smaller one is rejected by CHECK ck_room_occupied. */
    @CacheEvict(value = CacheNames.DASHBOARD_STATS, allEntries = true)
    @Transactional
    public RoomResponse updateRoom(Long id, RoomRequest request) {
        Room room = findRoom(id);
        if (request.capacity() < room.getOccupiedCount()) {
            throw new BusinessRuleException("Room has " + room.getOccupiedCount()
                    + " occupants; a " + request.roomType() + " room is too small");
        }
        copy(request, room);
        return RoomResponse.from(roomRepository.saveAndFlush(room));
    }

    private Room findRoom(Long id) {
        return roomRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Room", id));
    }

    private void copy(RoomRequest r, Room room) {
        room.setRoomNumber(r.roomNumber().trim().toUpperCase());
        room.setFloorNo(r.floorNo());
        room.setRoomType(r.roomType());
        room.setCapacity(r.capacity());
    }
}
