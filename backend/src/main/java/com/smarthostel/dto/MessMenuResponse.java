package com.smarthostel.dto;

import com.smarthostel.model.MessMenu;

public record MessMenuResponse(Long id, Long hostelId, String dayOfWeek, String mealType, String menuItems) {

    public static MessMenuResponse from(MessMenu m) {
        return new MessMenuResponse(m.getId(), m.getHostel().getId(), m.getDayOfWeek(), m.getMealType(), m.getMenuItems());
    }
}
