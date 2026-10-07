package com.smarthostel.service;

import com.smarthostel.dto.ListingRequest;
import com.smarthostel.dto.ListingResponse;
import com.smarthostel.dto.ListingStatusRequest;
import com.smarthostel.exception.BusinessRuleException;
import com.smarthostel.exception.ResourceNotFoundException;
import com.smarthostel.model.MarketplaceListing;
import com.smarthostel.repository.MarketplaceListingRepository;
import com.smarthostel.repository.StudentRepository;
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
public class MarketplaceService {

    private final MarketplaceListingRepository listingRepository;
    private final StudentRepository studentRepository;
    private final StudentService studentService;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<ListingResponse> search(String status, String category, Long sellerId, String q) {
        return listingRepository.search(blankToNull(status), blankToNull(category), sellerId, blankToNull(q)).stream()
                .map(ListingResponse::from).toList();
    }

    @Transactional
    public ListingResponse create(Long sellerId, ListingRequest request) {
        MarketplaceListing m = new MarketplaceListing();
        m.setSeller(studentService.findStudent(sellerId));
        m.setListedDate(LocalDate.now());
        m.setStatus("Available");
        copy(request, m);
        return ListingResponse.from(listingRepository.saveAndFlush(m));
    }

    @Transactional
    public ListingResponse update(Long id, ListingRequest request, AppUserDetails user) {
        MarketplaceListing m = findOwned(id, user);
        copy(request, m);
        return ListingResponse.from(listingRepository.saveAndFlush(m));
    }

    @Transactional
    public ListingResponse changeStatus(Long id, ListingStatusRequest request, AppUserDetails user) {
        MarketplaceListing m = findOwned(id, user);
        m.setStatus(request.status());
        m.setBuyer(null);
        if ("Sold".equals(request.status()) && blankToNull(request.buyerRegistrationNo()) != null) {
            m.setBuyer(studentRepository.findByRegistrationNoIgnoreCase(request.buyerRegistrationNo().trim())
                    .orElseThrow(() -> new BusinessRuleException("No student with registration number "
                            + request.buyerRegistrationNo())));
        }
        return ListingResponse.from(listingRepository.saveAndFlush(m));
    }

    @Transactional
    public ListingResponse attachImage(Long id, MultipartFile file, AppUserDetails user) {
        MarketplaceListing m = findOwned(id, user);
        m.setImagePath(fileStorageService.storeImage(file, "marketplace"));
        return ListingResponse.from(listingRepository.saveAndFlush(m));
    }

    @Transactional
    public void delete(Long id, AppUserDetails user) {
        listingRepository.delete(findOwned(id, user));
    }

    /** Sellers can change their own listings; admins/wardens can moderate any listing. */
    private MarketplaceListing findOwned(Long id, AppUserDetails user) {
        MarketplaceListing m = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", id));
        if (user.isStudent() && !m.getSeller().getId().equals(user.getId())) {
            throw new AccessDeniedException("Not your listing");
        }
        return m;
    }

    private void copy(ListingRequest r, MarketplaceListing m) {
        m.setTitle(r.title().trim());
        m.setDescription(r.description());
        m.setCategory(r.category());
        m.setPrice(r.price());
        m.setItemCondition(r.itemCondition());
    }
}
