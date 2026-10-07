package com.smarthostel.repository;

import com.smarthostel.model.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    String FETCH = " JOIN FETCH c.student s LEFT JOIN FETCH c.assignedTo st ";

    @Query("SELECT c FROM Complaint c" + FETCH + "WHERE s.id = :studentId ORDER BY c.createdDate DESC")
    List<Complaint> findByStudent(@Param("studentId") Long studentId);

    @Query("SELECT c FROM Complaint c" + FETCH + """
             WHERE st.id = :staffId
               AND (:status IS NULL OR c.status = :status)
             ORDER BY CASE c.status WHEN 'In Progress' THEN 1 WHEN 'Assigned' THEN 2 ELSE 3 END,
                      c.scheduledDate, c.createdDate DESC""")
    List<Complaint> findByAssignedStaff(@Param("staffId") Long staffId, @Param("status") String status);

    @Query("SELECT c FROM Complaint c" + FETCH + """
             WHERE (:status IS NULL OR c.status = :status)
               AND (:category IS NULL OR c.category = :category)
               AND (:q IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%'))
                               OR LOWER(s.registrationNo) LIKE LOWER(CONCAT('%', :q, '%'))
                               OR LOWER(c.description) LIKE LOWER(CONCAT('%', :q, '%')))
             ORDER BY c.createdDate DESC""")
    List<Complaint> search(@Param("status") String status, @Param("category") String category, @Param("q") String q);

    long countByStatus(String status);

    long countByStudentIdAndStatusNot(Long studentId, String status);
}
