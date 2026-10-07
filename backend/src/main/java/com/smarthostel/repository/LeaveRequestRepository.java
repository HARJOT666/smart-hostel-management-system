package com.smarthostel.repository;

import com.smarthostel.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    String FETCH = " JOIN FETCH l.student s LEFT JOIN FETCH l.approvedBy ";

    @Query("SELECT l FROM LeaveRequest l" + FETCH + "WHERE s.id = :studentId ORDER BY l.appliedDate DESC, l.id DESC")
    List<LeaveRequest> findByStudent(@Param("studentId") Long studentId);

    @Query("SELECT l FROM LeaveRequest l" + FETCH + """
             WHERE (:status IS NULL OR l.status = :status)
               AND (:q IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%'))
                               OR LOWER(s.registrationNo) LIKE LOWER(CONCAT('%', :q, '%')))
             ORDER BY CASE l.status WHEN 'Pending' THEN 0 ELSE 1 END, l.startDate DESC""")
    List<LeaveRequest> search(@Param("status") String status, @Param("q") String q);

    long countByStatus(String status);
}
