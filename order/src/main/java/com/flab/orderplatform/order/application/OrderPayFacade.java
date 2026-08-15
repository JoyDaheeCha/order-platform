package com.flab.orderplatform.order.application;

import com.flab.orderplatform.order.application.command.OrderPayCommand;
import com.flab.orderplatform.order.application.exception.OrderNotFoundException;
import com.flab.orderplatform.order.application.port.out.OrderRepository;
import com.flab.orderplatform.order.application.port.out.ProductRepository;
import com.flab.orderplatform.order.domain.Order;
import com.flab.orderplatform.order.domain.OrderItem;
import com.flab.orderplatform.order.domain.external.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * 주문 결제 로직 관련 use case Facade
 */
@Component
@RequiredArgsConstructor
public class OrderPayFacade {
    private final OrderCommandHandler orderCommandHandler;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public Order pay(String orderNumber) {
        var order = orderRepository.findWithOrderItemsByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));
        var productIds = order.getOrderItems()
                .stream()
                .map(OrderItem::getProductId)
                .toList();
        var products = productRepository.findAllByIdIn(productIds);
        var productMapCodeById = products.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        Product::getProductCode
                ));
        var command = OrderPayCommand.builder()
                .orderNumber(orderNumber)
                .productMapCodeById(productMapCodeById)
                .build();
        return orderCommandHandler.handle(command);
    }
}
