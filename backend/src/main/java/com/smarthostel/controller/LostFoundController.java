package com.smarthostel.controller;

import com.smarthostel.dto.LostFoundRequest;
import com.smarthostel.dto.LostFoundResponse;
import com.smarthostel.security.AppUserDetails;
import com.smarthostel.service.LostFoundService;
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
@RequestMapping("/api/lost-found")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT','ADMIN','WARDEN')")
public class LostFoundController {

    private final LostFoundService lostFoundService;

    @GetMapping
    public List<LostFoundResponse> list(@RequestParam(required = false) String type,
                                        @RequestParam(required = false) String status) {
        return lostFoundService.search(type, status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    public LostFoundResponse report(@Valid @RequestBody LostFoundRequest request,
                                    @AuthenticationPrincipal AppUserDetails user) {
        return lostFoundService.report(user.getId(), request);
    }

    @PutMapping("/{id}/claim")
    @PreAuthorize("hasRole('STUDENT')")
    public LostFoundResponse claim(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails user) {
        return lostFoundService.claim(id, user.getId());
    }

    @PutMapping("/{id}/close")
    public LostFoundResponse close(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails user) {
        return lostFoundService.close(id, user);
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public LostFoundResponse uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file,
                                         @AuthenticationPrincipal AppUserDetails user) {
        return lostFoundService.attachImage(id, file, user.getId());
    }
}
