package com.flab.orderplatform.order.application.port.out;

import com.flab.orderplatform.order.domain.Order;

import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);

    Optional<Order> findWithOrderItemsByOrderNumber(String orderNumber);
}
