package com.smarthostel.repository;

import com.smarthostel.model.FeePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeePaymentRepository extends JpaRepository<FeePayment, Long> {

    @Query("SELECT f FROM FeePayment f JOIN FETCH f.student s WHERE s.id = :studentId ORDER BY f.dueDate DESC, f.feeType")
    List<FeePayment> findByStudent(@Param("studentId") Long studentId);

    @Query("""
            SELECT f FROM FeePayment f JOIN FETCH f.student s
             WHERE (:semester IS NULL OR f.semester = :semester)
               AND (:outstandingOnly = false OR f.amountPaid < f.amountDue)
               AND (:q IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%'))
                               OR LOWER(s.registrationNo) LIKE LOWER(CONCAT('%', :q, '%')))
             ORDER BY f.dueDate DESC, s.registrationNo, f.feeType""")
    List<FeePayment> search(@Param("semester") String semester, @Param("outstandingOnly") boolean outstandingOnly,
                            @Param("q") String q);
}
