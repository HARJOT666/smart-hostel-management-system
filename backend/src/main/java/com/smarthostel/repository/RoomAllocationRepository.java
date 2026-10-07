package com.smarthostel.repository;

import com.smarthostel.model.RoomAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RoomAllocationRepository extends JpaRepository<RoomAllocation, Long> {

    String FETCH = " JOIN FETCH a.student s JOIN FETCH a.room r JOIN FETCH r.hostel h JOIN FETCH a.allocatedBy ";

    @Query("SELECT a FROM RoomAllocation a" + FETCH + "WHERE a.id = :id")
    Optional<RoomAllocation> findWithDetails(@Param("id") Long id);

    @Query("SELECT a FROM RoomAllocation a" + FETCH + "WHERE s.id = :studentId AND a.status = 'Active'")
    Optional<RoomAllocation> findActiveByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT a FROM RoomAllocation a" + FETCH + "WHERE s.id IN :studentIds AND a.status = 'Active'")
    List<RoomAllocation> findActiveByStudentIds(@Param("studentIds") Collection<Long> studentIds);

    @Query("SELECT a FROM RoomAllocation a" + FETCH + "WHERE s.id = :studentId ORDER BY a.allocationDate DESC")
    List<RoomAllocation> findHistoryByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT a FROM RoomAllocation a" + FETCH + "WHERE r.id = :roomId AND a.status = 'Active' ORDER BY a.bedNumber")
    List<RoomAllocation> findActiveByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT a FROM RoomAllocation a" + FETCH + """
             WHERE (:status IS NULL OR a.status = :status)
               AND (:hostelId IS NULL OR h.id = :hostelId)
               AND (:q IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%'))
                               OR LOWER(s.registrationNo) LIKE LOWER(CONCAT('%', :q, '%'))
                               OR LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :q, '%')))
             ORDER BY a.allocationDate DESC, a.id DESC""")
    List<RoomAllocation> search(@Param("status") String status, @Param("hostelId") Long hostelId, @Param("q") String q);
}
