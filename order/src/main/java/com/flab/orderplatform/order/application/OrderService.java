package com.flab.orderplatform.order.application;

import com.flab.orderplatform.order.application.port.out.OrderRepository;
import com.flab.orderplatform.order.domain.Order;
import com.flab.orderplatform.order.domain.OrderItem;
import com.flab.orderplatform.order.domain.OrderNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderNumberGenerator orderNumberGenerator;

    public Long createOrder(Long customerId, List<OrderItem> orderItems) {
        var orderNumber = orderNumberGenerator.generate();
        var order = Order.create(customerId, orderItems, orderNumber);
        return orderRepository.save(order);
    }
}
