package com.smarthostel.dto;

import com.smarthostel.model.FeePayment;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FeeResponse(Long id, Long studentId, String studentName, String registrationNo, String semester,
                          String feeType, BigDecimal amountDue, BigDecimal amountPaid, BigDecimal outstanding,
                          LocalDate dueDate, LocalDate paidDate, String paymentMode, String transactionRef,
                          String status) {

    public static FeeResponse from(FeePayment f) {
        return new FeeResponse(f.getId(), f.getStudent().getId(), f.getStudent().getName(),
                f.getStudent().getRegistrationNo(), f.getSemester(), f.getFeeType(), f.getAmountDue(),
                f.getAmountPaid(), f.getAmountDue().subtract(f.getAmountPaid()), f.getDueDate(), f.getPaidDate(),
                f.getPaymentMode(), f.getTransactionRef(), f.getStatus());
    }
}
