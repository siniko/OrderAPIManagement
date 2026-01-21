package com.sinik.orderapimanagement.api.dto;

import com.sinik.orderapimanagement.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
        @NotNull OrderStatus status
) {}
