package com.smarthostel.repository;

import com.smarthostel.model.MarketplaceListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MarketplaceListingRepository extends JpaRepository<MarketplaceListing, Long> {

    @Query("""
            SELECT m FROM MarketplaceListing m JOIN FETCH m.seller s LEFT JOIN FETCH m.buyer
             WHERE (:status IS NULL OR m.status = :status)
               AND (:category IS NULL OR m.category = :category)
               AND (:sellerId IS NULL OR s.id = :sellerId)
               AND (:q IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :q, '%')))
             ORDER BY m.listedDate DESC, m.id DESC""")
    List<MarketplaceListing> search(@Param("status") String status, @Param("category") String category,
                                    @Param("sellerId") Long sellerId, @Param("q") String q);
}
