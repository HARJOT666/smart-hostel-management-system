package com.smarthostel.service;

import com.smarthostel.dto.FeeResponse;
import com.smarthostel.dto.PaymentRequest;
import com.smarthostel.exception.BusinessRuleException;
import com.smarthostel.exception.ResourceNotFoundException;
import com.smarthostel.model.FeePayment;
import com.smarthostel.repository.FeePaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.smarthostel.service.AllocationService.blankToNull;

/** Fee status. Not cached: payment status must always be current. */
@Service
@RequiredArgsConstructor
public class FeeService {

    private final FeePaymentRepository feeRepository;

    @Transactional(readOnly = true)
    public List<FeeResponse> ofStudent(Long studentId) {
        return feeRepository.findByStudent(studentId).stream().map(FeeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<FeeResponse> search(String semester, boolean outstandingOnly, String q) {
        return feeRepository.search(blankToNull(semester), outstandingOnly, blankToNull(q)).stream()
                .map(FeeResponse::from).toList();
    }

    /** Records money received. Paying more than the due amount is refused (also a CHECK constraint). */
    @Transactional
    public FeeResponse recordPayment(Long paymentId, PaymentRequest request) {
        FeePayment f = feeRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Fee record", paymentId));
        BigDecimal newPaid = f.getAmountPaid().add(request.amount());
        if (newPaid.compareTo(f.getAmountDue()) > 0) {
            throw new BusinessRuleException("Payment exceeds the outstanding amount of "
                    + f.getAmountDue().subtract(f.getAmountPaid()));
        }
        f.setAmountPaid(newPaid);
        f.setPaidDate(LocalDate.now());
        f.setPaymentMode(request.paymentMode());
        f.setTransactionRef(blankToNull(request.transactionRef()));
        return FeeResponse.from(feeRepository.saveAndFlush(f));
    }
}
