package com.flab.orderplatform.order.application.port.out;

import com.flab.orderplatform.order.domain.Order;

public interface OrderRepository {
    Order save(Order order);
}
