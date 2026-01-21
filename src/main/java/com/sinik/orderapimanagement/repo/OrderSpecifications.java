package com.sinik.orderapimanagement.repo;

import com.sinik.orderapimanagement.domain.Order;
import com.sinik.orderapimanagement.domain.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class OrderSpecifications {
    private OrderSpecifications() {}

    public static Specification<Order> statusEquals(OrderStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Order> createdAtGte(Instant from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<Order> createdAtLte(Instant to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }
}
