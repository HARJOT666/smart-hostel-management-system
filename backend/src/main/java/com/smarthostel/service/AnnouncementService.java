package com.smarthostel.service;

import com.smarthostel.config.CacheNames;
import com.smarthostel.dto.AnnouncementRequest;
import com.smarthostel.dto.AnnouncementResponse;
import com.smarthostel.exception.ResourceNotFoundException;
import com.smarthostel.model.Announcement;
import com.smarthostel.repository.AnnouncementRepository;
import com.smarthostel.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final StaffRepository staffRepository;
    private final HostelService hostelService;

    /**
     * Active notices for one hostel (plus global ones). hostelId = null -> all notices.
     * Cached in Redis per hostel: shms:announcements::<hostelId or 'all'>, TTL 10 min.
     */
    @Cacheable(value = CacheNames.ANNOUNCEMENTS, key = "#hostelId == null ? 'all' : #hostelId")
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getAnnouncements(Long hostelId) {
        log.info("CACHE MISS: loading announcements (hostel {}) from Oracle", hostelId);
        LocalDate today = LocalDate.now();
        return announcementRepository.findForHostel(hostelId).stream()
                .filter(a -> a.getExpiryDate() == null || !a.getExpiryDate().isBefore(today))
                .map(AnnouncementResponse::from)
                .toList();
    }

    /** Every write clears ALL announcement cache entries (a global notice affects every hostel's list). */
    @CacheEvict(value = CacheNames.ANNOUNCEMENTS, allEntries = true)
    @Transactional
    public AnnouncementResponse create(AnnouncementRequest request, Long staffId) {
        Announcement a = new Announcement();
        a.setPostedBy(staffRepository.getReferenceById(staffId));
        a.setPostedDate(LocalDateTime.now());
        copy(request, a);
        announcementRepository.saveAndFlush(a);
        return AnnouncementResponse.from(findAnnouncement(a.getId()));
    }

    @CacheEvict(value = CacheNames.ANNOUNCEMENTS, allEntries = true)
    @Transactional
    public AnnouncementResponse update(Long id, AnnouncementRequest request) {
        Announcement a = findAnnouncement(id);
        copy(request, a);
        announcementRepository.saveAndFlush(a);
        return AnnouncementResponse.from(a);
    }

    @CacheEvict(value = CacheNames.ANNOUNCEMENTS, allEntries = true)
    @Transactional
    public void delete(Long id) {
        announcementRepository.delete(findAnnouncement(id));
    }

    private Announcement findAnnouncement(Long id) {
        return announcementRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Announcement", id));
    }

    private void copy(AnnouncementRequest r, Announcement a) {
        a.setTitle(r.title().trim());
        a.setBody(r.body().trim());
        a.setCategory(r.category());
        a.setHostel(r.hostelId() == null ? null : hostelService.findHostel(r.hostelId()));
        a.setExpiryDate(r.expiryDate());
        a.setIsPinned(r.pinned() ? 'Y' : 'N');
    }
}
