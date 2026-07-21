package com.flab.orderplatform.order.application.port.out;

import com.flab.orderplatform.order.domain.Order;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository {
    Order save(Order order);
}
