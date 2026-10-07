package com.smarthostel.repository;

import com.smarthostel.model.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    /** hostelId = null returns everything (admin view); otherwise that hostel's notices plus global ones. */
    @Query("""
            SELECT a FROM Announcement a JOIN FETCH a.postedBy LEFT JOIN FETCH a.hostel h
             WHERE :hostelId IS NULL OR h IS NULL OR h.id = :hostelId
             ORDER BY a.isPinned DESC, a.postedDate DESC""")
    List<Announcement> findForHostel(@Param("hostelId") Long hostelId);
}
