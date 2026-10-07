package com.smarthostel.dto;

import com.smarthostel.model.InventoryItem;

import java.time.LocalDate;

public record InventoryResponse(Long id, Long hostelId, String hostelName, String itemName, String category,
                                Integer quantityTotal, Integer quantityDamaged, Integer quantityUsable,
                                LocalDate lastChecked) {

    public static InventoryResponse from(InventoryItem i) {
        return new InventoryResponse(i.getId(), i.getHostel().getId(), i.getHostel().getHostelName(),
                i.getItemName(), i.getCategory(), i.getQuantityTotal(), i.getQuantityDamaged(),
                i.getQuantityTotal() - i.getQuantityDamaged(), i.getLastChecked());
    }
}
