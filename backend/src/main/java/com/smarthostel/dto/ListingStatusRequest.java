package com.smarthostel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Seller marks an item Sold (optionally to a buyer) or Withdrawn. */
public record ListingStatusRequest(@NotBlank @Pattern(regexp = "Available|Sold|Withdrawn") String status,
                                   String buyerRegistrationNo) {
}
