package com.smarthostel.dto;

import com.smarthostel.model.Hostel;

/** Only fields that rarely change, so it is safe to cache. Occupancy is NOT included. */
public record HostelResponse(Long id, String hostelName, String genderType, String location, Integer totalFloors) {

    public static HostelResponse from(Hostel h) {
        return new HostelResponse(h.getId(), h.getHostelName(), h.getGenderType(), h.getLocation(), h.getTotalFloors());
    }
}
