package com.smarthostel.service;

import com.smarthostel.dto.LostFoundRequest;
import com.smarthostel.dto.LostFoundResponse;
import com.smarthostel.exception.BusinessRuleException;
import com.smarthostel.exception.ResourceNotFoundException;
import com.smarthostel.model.LostFoundItem;
import com.smarthostel.repository.LostFoundItemRepository;
import com.smarthostel.security.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

import static com.smarthostel.service.AllocationService.blankToNull;

@Service
@RequiredArgsConstructor
public class LostFoundService {

    private final LostFoundItemRepository lostFoundRepository;
    private final StudentService studentService;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<LostFoundResponse> search(String type, String status) {
        return lostFoundRepository.search(blankToNull(type), blankToNull(status)).stream()
                .map(LostFoundResponse::from).toList();
    }

    @Transactional
    public LostFoundResponse report(Long studentId, LostFoundRequest r) {
        LostFoundItem item = new LostFoundItem();
        item.setReportedBy(studentService.findStudent(studentId));
        item.setReportType(r.reportType());
        item.setItemName(r.itemName().trim());
        item.setDescription(r.description());
        item.setLocation(r.location());
        item.setReportDate(LocalDate.now());
        item.setStatus("Open");
        return LostFoundResponse.from(lostFoundRepository.saveAndFlush(item));
    }

    /** Another student says "this is mine" (found item) or "I found it" (lost item). */
    @Transactional
    public LostFoundResponse claim(Long id, Long studentId) {
        LostFoundItem item = findItem(id);
        if (item.getReportedBy().getId().equals(studentId)) {
            throw new BusinessRuleException("You cannot claim your own report");
        }
        if (!"Open".equals(item.getStatus())) {
            throw new BusinessRuleException("Item is already " + item.getStatus());
        }
        item.setClaimedBy(studentService.findStudent(studentId));
        item.setStatus("Claimed");
        return LostFoundResponse.from(lostFoundRepository.saveAndFlush(item));
    }

    @Transactional
    public LostFoundResponse close(Long id, AppUserDetails user) {
        LostFoundItem item = findItem(id);
        if (user.isStudent() && !item.getReportedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only the reporter or a warden can close this");
        }
        item.setStatus("Closed");
        return LostFoundResponse.from(lostFoundRepository.saveAndFlush(item));
    }

    @Transactional
    public LostFoundResponse attachImage(Long id, MultipartFile file, Long studentId) {
        LostFoundItem item = findItem(id);
        if (!item.getReportedBy().getId().equals(studentId)) {
            throw new AccessDeniedException("Not your report");
        }
        item.setImagePath(fileStorageService.storeImage(file, "lostfound"));
        return LostFoundResponse.from(lostFoundRepository.saveAndFlush(item));
    }

    private LostFoundItem findItem(Long id) {
        return lostFoundRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Item", id));
    }
}
