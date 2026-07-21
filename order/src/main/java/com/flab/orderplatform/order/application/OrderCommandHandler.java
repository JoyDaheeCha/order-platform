package com.flab.orderplatform.order.application;

import com.flab.orderplatform.order.application.command.OrderCreateCommand;
import com.flab.orderplatform.order.application.port.out.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderCommandHandler {
    private final OrderRepository orderRepository;

    @Transactional
    public Long handle(String orderNumber, Map<String, Long> priceMap, OrderCreateCommand command) {
        return orderRepository.save(command.createOrder(orderNumber, priceMap));
    }
}
