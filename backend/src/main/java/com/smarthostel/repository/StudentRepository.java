package com.smarthostel.repository;

import com.smarthostel.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByRegistrationNoIgnoreCase(String registrationNo);

    boolean existsByRegistrationNoIgnoreCase(String registrationNo);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            SELECT s FROM Student s
             WHERE (:q IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%'))
                               OR LOWER(s.registrationNo) LIKE LOWER(CONCAT('%', :q, '%')))
               AND (:year IS NULL OR s.year = :year)
               AND (:gender IS NULL OR s.gender = :gender)
             ORDER BY s.registrationNo""")
    List<Student> search(@Param("q") String q, @Param("year") Integer year, @Param("gender") String gender);

    /** Students without an active room allocation (waiting for a room). */
    @Query("""
            SELECT s FROM Student s
             WHERE NOT EXISTS (SELECT a FROM RoomAllocation a
                                WHERE a.student = s AND a.status = 'Active')
             ORDER BY s.year, s.registrationNo""")
    List<Student> findWaitingForRoom();
}
