package com.sinik.orderapimanagement.repo;

import com.sinik.orderapimanagement.domain.Order;
import com.sinik.orderapimanagement.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    Page<Order> findByCreatedAtBetween(Instant from, Instant to, Pageable pageable);

    Page<Order> findByStatusAndCreatedAtBetween(OrderStatus status, Instant from, Instant to, Pageable pageable);
}
