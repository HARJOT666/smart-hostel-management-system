package com.smarthostel.repository;

import com.smarthostel.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    @Query("""
            SELECT i FROM InventoryItem i JOIN FETCH i.hostel h
             WHERE (:hostelId IS NULL OR h.id = :hostelId)
             ORDER BY h.id, i.category, i.itemName""")
    List<InventoryItem> search(@Param("hostelId") Long hostelId);
}
