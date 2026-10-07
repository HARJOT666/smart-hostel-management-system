package com.smarthostel.dto;

import jakarta.validation.constraints.NotBlank;

/** QR code value read at the hostel gate. */
public record ScanRequest(@NotBlank String qrCode) {
}
