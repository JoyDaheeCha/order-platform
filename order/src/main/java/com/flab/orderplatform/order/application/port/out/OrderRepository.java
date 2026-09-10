package com.flab.orderplatform.order.application.port.out;

import com.flab.orderplatform.order.domain.Order;
import com.flab.orderplatform.order.domain.status.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);

    Optional<Order> findWithOrderItemsByOrderNumber(String orderNumber);

    List<Order> findReleaseTarget(LocalDateTime reservedAtThreshold, OrderStatus orderStatus);

    Optional<Order> findByOrderNumber(String orderNumber);
}
