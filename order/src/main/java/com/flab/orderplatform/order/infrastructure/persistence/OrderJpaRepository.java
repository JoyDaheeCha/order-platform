package com.flab.orderplatform.order.infrastructure.persistence;

import com.flab.orderplatform.order.domain.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {
    @EntityGraph(attributePaths = {"orderItems"})
    Optional<Order> findWithOrderItemsByOrderNumber(String orderNumber);
}
