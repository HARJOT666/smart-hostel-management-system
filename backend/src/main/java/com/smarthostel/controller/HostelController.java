package com.smarthostel.controller;

import com.smarthostel.dto.HostelRequest;
import com.smarthostel.dto.HostelResponse;
import com.smarthostel.service.HostelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hostels")
@RequiredArgsConstructor
public class HostelController {

    private final HostelService hostelService;

    @GetMapping
    public List<HostelResponse> getAll() {
        return hostelService.getAllHostels();
    }

    @GetMapping("/{id}")
    public HostelResponse getOne(@PathVariable Long id) {
        return hostelService.getHostel(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public HostelResponse create(@Valid @RequestBody HostelRequest request) {
        return hostelService.createHostel(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public HostelResponse update(@PathVariable Long id, @Valid @RequestBody HostelRequest request) {
        return hostelService.updateHostel(id, request);
    }
}
