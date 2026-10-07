package com.smarthostel.controller;

import com.smarthostel.dto.ListingRequest;
import com.smarthostel.dto.ListingResponse;
import com.smarthostel.dto.ListingStatusRequest;
import com.smarthostel.security.AppUserDetails;
import com.smarthostel.service.MarketplaceService;
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
@RequestMapping("/api/marketplace")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT','ADMIN','WARDEN')")
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    @GetMapping
    public List<ListingResponse> browse(@RequestParam(required = false) String status,
                                        @RequestParam(required = false) String category,
                                        @RequestParam(required = false) String q) {
        return marketplaceService.search(status, category, null, q);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public List<ListingResponse> mine(@AuthenticationPrincipal AppUserDetails user) {
        return marketplaceService.search(null, null, user.getId(), null);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    public ListingResponse create(@Valid @RequestBody ListingRequest request,
                                  @AuthenticationPrincipal AppUserDetails user) {
        return marketplaceService.create(user.getId(), request);
    }

    @PutMapping("/{id}")
    public ListingResponse update(@PathVariable Long id, @Valid @RequestBody ListingRequest request,
                                  @AuthenticationPrincipal AppUserDetails user) {
        return marketplaceService.update(id, request, user);
    }

    @PutMapping("/{id}/status")
    public ListingResponse changeStatus(@PathVariable Long id, @Valid @RequestBody ListingStatusRequest request,
                                        @AuthenticationPrincipal AppUserDetails user) {
        return marketplaceService.changeStatus(id, request, user);
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ListingResponse uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file,
                                       @AuthenticationPrincipal AppUserDetails user) {
        return marketplaceService.attachImage(id, file, user);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails user) {
        marketplaceService.delete(id, user);
    }
}
