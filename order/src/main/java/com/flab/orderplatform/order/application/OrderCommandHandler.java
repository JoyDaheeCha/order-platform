package com.flab.orderplatform.order.application;

import com.flab.orderplatform.order.application.annotation.OrderTransactional;
import com.flab.orderplatform.order.application.command.OrderCreateCommand;
import com.flab.orderplatform.order.application.port.out.OrderRepository;
import com.flab.orderplatform.order.domain.Order;
import com.flab.orderplatform.order.domain.external.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderCommandHandler {
    private final OrderRepository orderRepository;

    @OrderTransactional
    public Order handle(String orderNumber, Map<String, Product> productMap, OrderCreateCommand command) {
        return orderRepository.save(command.createOrder(orderNumber, productMap));
    }
}
