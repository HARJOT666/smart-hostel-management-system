package com.smarthostel.repository;

import com.smarthostel.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    @Query("""
            SELECT r FROM Room r JOIN FETCH r.hostel h
             WHERE (:hostelId IS NULL OR h.id = :hostelId)
               AND (:roomType IS NULL OR r.roomType = :roomType)
             ORDER BY h.id, r.roomNumber""")
    List<Room> search(@Param("hostelId") Long hostelId, @Param("roomType") String roomType);

    /** Rooms with at least one free bed, using the Oracle function fn_available_beds. */
    @Query(nativeQuery = true, value = """
            SELECT r.* FROM room r JOIN hostel h ON h.hostel_id = r.hostel_id
             WHERE fn_available_beds(r.room_id) > 0
               AND h.gender_type = :gender
               AND (:hostelId IS NULL OR r.hostel_id = :hostelId)
             ORDER BY r.hostel_id, r.room_number""")
    List<Room> findAvailableRooms(@Param("gender") String gender, @Param("hostelId") Long hostelId);

    boolean existsByHostelIdAndRoomNumberIgnoreCase(Long hostelId, String roomNumber);
}
