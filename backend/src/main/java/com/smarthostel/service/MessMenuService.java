package com.smarthostel.service;

import com.smarthostel.config.CacheNames;
import com.smarthostel.dto.MessMenuResponse;
import com.smarthostel.dto.MessMenuUpdateRequest;
import com.smarthostel.exception.ResourceNotFoundException;
import com.smarthostel.model.MessMenu;
import com.smarthostel.repository.MessMenuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessMenuService {

    private static final List<String> DAYS = List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");
    private static final List<String> MEALS = List.of("Breakfast", "Lunch", "Snacks", "Dinner");

    private final MessMenuRepository messMenuRepository;

    /** Weekly menu of a hostel. Cached per hostel (shms:messMenu::<hostelId>, TTL 6 hours). */
    @Cacheable(value = CacheNames.MESS_MENU, key = "#hostelId")
    @Transactional(readOnly = true)
    public List<MessMenuResponse> getMenu(Long hostelId) {
        log.info("CACHE MISS: loading mess menu of hostel {} from Oracle", hostelId);
        return messMenuRepository.findByHostelId(hostelId).stream()
                .sorted(Comparator.comparing((MessMenu m) -> DAYS.indexOf(m.getDayOfWeek()))
                        .thenComparing(m -> MEALS.indexOf(m.getMealType())))
                .map(MessMenuResponse::from)
                .toList();
    }

    /** Editing one meal removes that hostel's cached menu. */
    @CacheEvict(value = CacheNames.MESS_MENU, key = "#result.hostelId()")
    @Transactional
    public MessMenuResponse updateMeal(Long menuId, MessMenuUpdateRequest request) {
        MessMenu m = messMenuRepository.findById(menuId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item", menuId));
        m.setMenuItems(request.menuItems().trim());
        return MessMenuResponse.from(messMenuRepository.saveAndFlush(m));
    }
}
