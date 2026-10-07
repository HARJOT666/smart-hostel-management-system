package com.smarthostel.service;

import com.smarthostel.dto.InventoryRequest;
import com.smarthostel.dto.InventoryResponse;
import com.smarthostel.exception.BusinessRuleException;
import com.smarthostel.exception.ResourceNotFoundException;
import com.smarthostel.model.InventoryItem;
import com.smarthostel.repository.InventoryItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryItemRepository inventoryRepository;
    private final HostelService hostelService;

    @Transactional(readOnly = true)
    public List<InventoryResponse> search(Long hostelId) {
        return inventoryRepository.search(hostelId).stream().map(InventoryResponse::from).toList();
    }

    @Transactional
    public InventoryResponse create(InventoryRequest request) {
        InventoryItem item = new InventoryItem();
        copy(request, item);
        return InventoryResponse.from(inventoryRepository.saveAndFlush(item));
    }

    @Transactional
    public InventoryResponse update(Long id, InventoryRequest request) {
        InventoryItem item = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item", id));
        copy(request, item);
        return InventoryResponse.from(inventoryRepository.saveAndFlush(item));
    }

    @Transactional
    public void delete(Long id) {
        inventoryRepository.deleteById(id);
    }

    private void copy(InventoryRequest r, InventoryItem item) {
        if (r.quantityDamaged() > r.quantityTotal()) {
            throw new BusinessRuleException("Damaged quantity cannot be more than the total");
        }
        item.setHostel(hostelService.findHostel(r.hostelId()));
        item.setItemName(r.itemName().trim());
        item.setCategory(r.category());
        item.setQuantityTotal(r.quantityTotal());
        item.setQuantityDamaged(r.quantityDamaged());
        item.setLastChecked(LocalDate.now());
    }
}
