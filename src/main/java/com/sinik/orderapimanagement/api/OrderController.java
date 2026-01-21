package com.sinik.orderapimanagement.api;

import com.sinik.orderapimanagement.api.dto.CreateOrderRequest;
import com.sinik.orderapimanagement.api.dto.OrderResponse;
import com.sinik.orderapimanagement.api.dto.PageResponse;
import com.sinik.orderapimanagement.api.dto.UpdateStatusRequest;
import com.sinik.orderapimanagement.domain.Order;
import com.sinik.orderapimanagement.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.sinik.orderapimanagement.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest req) {
        return toResponse(service.create(req.customerId()));
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id) {
        return toResponse(service.get(id));
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateStatusRequest req) {
        return toResponse(service.updateStatus(id, req.status()));
    }

    @GetMapping
    public PageResponse<OrderResponse> search(@RequestParam(required = false) OrderStatus status,
                                              @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to, Pageable pageable) {
        return PageResponse.from(service.search(status, from, to, pageable).map(OrderController::toResponse));
    }

    private static OrderResponse toResponse(Order o) {
        return new OrderResponse(
                o.getId(),
                o.getStatus(),
                o.getCustomerId(),
                o.getCreatedAt(),
                o.getUpdatedAt()
        );
    }
}
