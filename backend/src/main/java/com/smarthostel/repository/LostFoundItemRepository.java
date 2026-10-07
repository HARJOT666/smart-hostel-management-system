package com.smarthostel.repository;

import com.smarthostel.model.LostFoundItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LostFoundItemRepository extends JpaRepository<LostFoundItem, Long> {

    @Query("""
            SELECT i FROM LostFoundItem i JOIN FETCH i.reportedBy LEFT JOIN FETCH i.claimedBy
             WHERE (:type IS NULL OR i.reportType = :type)
               AND (:status IS NULL OR i.status = :status)
             ORDER BY i.reportDate DESC, i.id DESC""")
    List<LostFoundItem> search(@Param("type") String type, @Param("status") String status);
}
