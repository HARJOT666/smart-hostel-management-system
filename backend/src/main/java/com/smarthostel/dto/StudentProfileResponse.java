package com.smarthostel.dto;

import java.util.List;

/** Everything shown on the student's "My Room" page. */
public record StudentProfileResponse(StudentResponse student, RoomResponse room, HostelResponse hostel,
                                     List<String> roommates, List<AllocationResponse> history) {
}
