package com.smarthostel.repository;

import com.smarthostel.dto.MaintenanceOption;
import com.smarthostel.model.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    Optional<Staff> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            SELECT s FROM Staff s JOIN FETCH s.hostel h
             WHERE (:role IS NULL OR s.role = :role)
               AND (:hostelId IS NULL OR h.id = :hostelId)
             ORDER BY h.id, s.role, s.name""")
    List<Staff> search(@Param("role") String role, @Param("hostelId") Long hostelId);

    /** Wardens and maintenance staff of one hostel, used as emergency contacts. */
    @Query("""
            SELECT s FROM Staff s JOIN FETCH s.hostel h
             WHERE h.id = :hostelId AND s.role IN ('Warden', 'Maintenance')
             ORDER BY s.role DESC, s.specialization, s.name""")
    List<Staff> findContactsForHostel(@Param("hostelId") Long hostelId);

    /**
     * Maintenance staff who may take a complaint of this category in this hostel,
     * least busy first. The workload comes from the Oracle function fn_staff_open_workload.
     */
    @Query(nativeQuery = true, value = """
            SELECT s.staff_id AS id, s.name AS name, s.specialization AS specialization,
                   fn_staff_open_workload(s.staff_id) AS workload
              FROM staff s
             WHERE s.role = 'Maintenance'
               AND (:hostelId IS NULL OR s.hostel_id = :hostelId)
               AND (:category = 'Other' OR s.specialization = :category)
             ORDER BY workload, s.name""")
    List<MaintenanceOption> findMaintenanceOptions(@Param("hostelId") Long hostelId,
                                                   @Param("category") String category);
}
