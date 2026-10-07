package com.smarthostel.repository;

import com.smarthostel.model.VisitorPass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VisitorPassRepository extends JpaRepository<VisitorPass, Long> {

    String FETCH = " JOIN FETCH v.student s LEFT JOIN FETCH v.approvedBy ";

    @Query("SELECT v FROM VisitorPass v" + FETCH + "WHERE s.id = :studentId ORDER BY v.visitDate DESC, v.id DESC")
    List<VisitorPass> findByStudent(@Param("studentId") Long studentId);

    @Query("SELECT v FROM VisitorPass v" + FETCH + """
             WHERE (:status IS NULL OR v.status = :status)
               AND (:visitDate IS NULL OR v.visitDate = :visitDate)
               AND (:q IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%'))
                               OR LOWER(v.visitorName) LIKE LOWER(CONCAT('%', :q, '%'))
                               OR LOWER(s.registrationNo) LIKE LOWER(CONCAT('%', :q, '%')))
             ORDER BY CASE v.status WHEN 'Pending' THEN 0 ELSE 1 END, v.visitDate DESC""")
    List<VisitorPass> search(@Param("status") String status, @Param("visitDate") LocalDate visitDate, @Param("q") String q);

    @Query("SELECT v FROM VisitorPass v" + FETCH + "WHERE v.qrCode = :qrCode")
    Optional<VisitorPass> findByQrCode(@Param("qrCode") String qrCode);

    long countByStatus(String status);
}
