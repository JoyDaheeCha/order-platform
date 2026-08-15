package com.flab.orderplatform.order.application;

import com.flab.orderplatform.order.application.annotation.OrderTransactional;
import com.flab.orderplatform.order.application.command.OrderCreateCommand;
import com.flab.orderplatform.order.application.command.OrderPayCommand;
import com.flab.orderplatform.order.application.exception.DuplicatedProductException;
import com.flab.orderplatform.order.application.exception.OrderNotFoundException;
import com.flab.orderplatform.order.application.exception.ProductNotFoundException;
import com.flab.orderplatform.order.application.port.out.OrderRepository;
import com.flab.orderplatform.order.domain.Order;
import com.flab.orderplatform.order.domain.OrderItem;
import com.flab.orderplatform.order.domain.external.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderCommandHandler {
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static void validateNoDuplicatedProductCode(List<OrderCreateCommand.OrderItemDto> orderItems) {
        var duplicated = orderItems.stream()
                .collect(Collectors.groupingBy(OrderCreateCommand.OrderItemDto::productCode, Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!duplicated.isEmpty()) {
            throw new DuplicatedProductException(duplicated);
        }
    }

    @OrderTransactional
    public Order handle(String orderNumber, Map<String, Product> productMap, OrderCreateCommand command) {
        // 유효성 검증 : 주문 내 상품정보는 중복 금지
        validateNoDuplicatedProductCode(command.getOrderItems());

        var orderItems = command.getOrderItems().stream().map(item -> {
                    var product = productMap.get(item.productCode());
                    if (product == null) {
                        throw new ProductNotFoundException(item.productCode());
                    }
                    return OrderItem.builder()
                            .quantity(item.quantity())
                            .name(item.name())
                            .productId(product.getId())
                            .price(product.getPrice())
                            .build();
                })
                .toList();
        var order = orderRepository.save(command.createOrder(orderNumber, orderItems));
        order.pullDomainEventIfPresent()
                .ifPresent(eventPublisher::publishEvent);
        return order;
    }

    @OrderTransactional
    public Order handle(OrderPayCommand command) {
        var order = orderRepository.findWithOrderItemsByOrderNumber(command.orderNumber())
                .orElseThrow(() -> new OrderNotFoundException(command.orderNumber()));
        var result = command.pay(order);
        order.pullDomainEventIfPresent()
                .ifPresent(eventPublisher::publishEvent);
        return orderRepository.save(result);
    }
}
