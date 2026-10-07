package com.smarthostel.controller;

import com.smarthostel.dto.AllocationResponse;
import com.smarthostel.dto.RoomRequest;
import com.smarthostel.dto.RoomResponse;
import com.smarthostel.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public List<RoomResponse> search(@RequestParam(required = false) Long hostelId,
                                     @RequestParam(required = false) String roomType,
                                     @RequestParam(defaultValue = "false") boolean availableOnly) {
        return roomService.searchRooms(hostelId, roomType, availableOnly);
    }

    /** Rooms a student of this gender can be allocated to (free beds only). */
    @GetMapping("/available")
    public List<RoomResponse> available(@RequestParam String gender, @RequestParam(required = false) Long hostelId) {
        return roomService.findAvailableRooms(gender, hostelId);
    }

    @GetMapping("/{id}")
    public RoomResponse getOne(@PathVariable Long id) {
        return roomService.getRoom(id);
    }

    @GetMapping("/{id}/occupants")
    public List<AllocationResponse> occupants(@PathVariable Long id) {
        return roomService.getOccupants(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public RoomResponse create(@Valid @RequestBody RoomRequest request) {
        return roomService.createRoom(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public RoomResponse update(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        return roomService.updateRoom(id, request);
    }
}
