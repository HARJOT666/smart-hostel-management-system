package com.smarthostel.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** Admin records a payment received from a student. */
public record PaymentRequest(
        @NotNull @DecimalMin(value = "1.00") BigDecimal amount,
        @NotBlank @Pattern(regexp = "Online|Card|Cash|DD") String paymentMode,
        @Size(max = 30) String transactionRef) {
}
