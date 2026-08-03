package com.flab.orderplatform.order.application;

import com.flab.orderplatform.order.application.annotation.OrderTransactional;
import com.flab.orderplatform.order.application.command.OrderCreateCommand;
import com.flab.orderplatform.order.application.port.out.OrderRepository;
import com.flab.orderplatform.order.domain.Order;
import com.flab.orderplatform.order.domain.OrderItem;
import com.flab.orderplatform.order.domain.external.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderCommandHandler {
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    @OrderTransactional
    public Order handle(String orderNumber, Map<String, Product> productMap, OrderCreateCommand command) {
        var orderItems = command.getOrderItems().stream().map(item -> {
                    var product = productMap.get(item.productCode());
                    return OrderItem.builder()
                            .quantity(item.quantity())
                            .name(item.name())
                            .productId(product.getId())
                            .price(product.getPrice())
                            .quantity(item.quantity())
                            .build();
                })
                .toList();
        var order = orderRepository.save(command.createOrder(orderNumber, orderItems));
        eventPublisher.publishEvent(order.pullDomainEvent());
        return order;
    }
}
