package com.sinik.orderapimanagement.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequest(
        @NotBlank String customerId
) {}
