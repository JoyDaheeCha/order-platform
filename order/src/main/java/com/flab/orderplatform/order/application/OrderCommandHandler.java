package com.flab.orderplatform.order.application;

import com.flab.orderplatform.order.application.command.OrderCreateCommand;
import com.flab.orderplatform.order.application.port.out.OrderRepository;
import com.flab.orderplatform.order.domain.Order;
import com.flab.orderplatform.order.domain.external.Product;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderCommandHandler {
    private final OrderRepository orderRepository;

    @Transactional
    public Order handle(String orderNumber, Map<String, Product> productMap, OrderCreateCommand command) {
        return orderRepository.save(command.createOrder(orderNumber, productMap));
    }
}
