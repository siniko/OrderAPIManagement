package com.sinik.orderapimanagement.service;

import com.sinik.orderapimanagement.domain.Order;
import com.sinik.orderapimanagement.domain.OrderStatus;
import com.sinik.orderapimanagement.error.InvalidStatusTransitionException;
import com.sinik.orderapimanagement.error.OrderNotFoundException;
import com.sinik.orderapimanagement.notification.event.OrderCreatedEvent;
import com.sinik.orderapimanagement.notification.event.OrderStatusChangedEvent;
import com.sinik.orderapimanagement.repo.OrderRepository;
import com.sinik.orderapimanagement.repo.OrderSpecifications;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository repo;
    private final ApplicationEventPublisher publisher;

    public OrderService(OrderRepository repo, ApplicationEventPublisher publisher) {
        this.repo = repo;
        this.publisher = publisher;
    }

    @Transactional
    public Order create(String customerId) {
        Order saved = repo.save(new Order(customerId));
        publisher.publishEvent(new OrderCreatedEvent(saved.getId()));
        return saved;
    }

    @Transactional(readOnly = true)
    public Order get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional
    public Order updateStatus(UUID id, OrderStatus newStatus) {
        Order o = repo.findById(id).orElseThrow(() -> new OrderNotFoundException(id));

        OrderStatus current = o.getStatus();
        validateTransition(id, current, newStatus);

        o.setStatus(newStatus);
        Order saved = repo.save(o);

        publisher.publishEvent(new OrderStatusChangedEvent(saved.getId(), current, newStatus));
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Order> search(OrderStatus status, Instant from, Instant to, Pageable pageable) {
        Specification<Order> spec = Specification.<Order>allOf();

        if (status != null) spec = spec.and(OrderSpecifications.statusEquals(status));
        if (from != null) spec = spec.and(OrderSpecifications.createdAtGte(from));
        if (to != null) spec = spec.and(OrderSpecifications.createdAtLte(to));

        return repo.findAll(spec, pageable);
    }

    private void validateTransition(UUID id, OrderStatus from, OrderStatus to) {
        if (from == to) return;

        // Only allow CREATED -> CANCELLED or CREATED -> COMPLETED
        if (from == OrderStatus.CREATED &&
                (to == OrderStatus.CANCELLED || to == OrderStatus.COMPLETED)) {
            return;
        }

        throw new InvalidStatusTransitionException(id, from, to);
    }
}
