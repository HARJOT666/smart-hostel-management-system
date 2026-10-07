package com.smarthostel.dto;

import com.smarthostel.model.MarketplaceListing;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ListingResponse(Long id, Long sellerId, String sellerName, String sellerPhone, String sellerEmail,
                              String buyerName, String title, String description, String category, BigDecimal price,
                              String itemCondition, String status, String imageUrl, LocalDate listedDate) {

    public static ListingResponse from(MarketplaceListing m) {
        return new ListingResponse(m.getId(), m.getSeller().getId(), m.getSeller().getName(),
                m.getSeller().getPhone(), m.getSeller().getEmail(),
                m.getBuyer() == null ? null : m.getBuyer().getName(), m.getTitle(), m.getDescription(),
                m.getCategory(), m.getPrice(), m.getItemCondition(), m.getStatus(),
                ComplaintResponse.toUrl(m.getImagePath()), m.getListedDate());
    }
}
