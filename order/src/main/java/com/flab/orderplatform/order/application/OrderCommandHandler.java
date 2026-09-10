package com.flab.orderplatform.order.application;

import com.flab.orderplatform.order.application.annotation.OrderTransactional;
import com.flab.orderplatform.order.application.command.*;
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

    /**
     * 주문 생성
     *
     * @param orderNumber 주문번호
     * @param productMap  상품 코드별 상품 정보
     * @param command     주문 생성 명령
     * @return 주문
     */
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
                            .productCode(product.getProductCode())
                            .price(product.getPrice())
                            .build();
                })
                .toList();
        var order = orderRepository.save(command.createOrder(orderNumber, orderItems));
        order.pullDomainEventIfPresent()
                .ifPresent(eventPublisher::publishEvent);
        return order;
    }

    /**
     * 주문 결제
     *
     * @param command 주문 결제 명령
     *                O98
     */
    @OrderTransactional
    public Order handle(OrderPayCommand command) {
        var order = getOrder(command.orderNumber());
        var result = command.pay(order);
        result.pullDomainEventIfPresent()
                .ifPresent(eventPublisher::publishEvent);
        return orderRepository.save(result);
    }

    private Order getOrder(String orderNumber) {
        return orderRepository.findWithOrderItemsByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));
    }

    /**
     * 주문 결제 준비
     *
     * @param command 주문 결제 준비 명령
     * @return 주문
     */
    @OrderTransactional
    public Order handle(OrderPreparePaymentCommand command) {
        var order = getOrderByOrderNumber(command.orderNumber());
        var result = command.preparePayment(order);
        result.pullDomainEventIfPresent()
                .ifPresent(eventPublisher::publishEvent);
        return orderRepository.save(result);
    }

    /**
     * 재고 부족으로 인한 주문 실패 처리
     *
     * @param command 주문 실패 명령
     * @return 주문
     */
    @OrderTransactional
    public Order handle(OrderFailByInventoryShortageCommand command) {
        var order = getOrderByOrderNumber(command.orderNumber());
        var result = command.fail(order);
        return orderRepository.save(result);
    }

    @OrderTransactional
    public Order handle(OrderFailByPaymentTimeoutCommand command) {
        var order = getOrderByOrderNumber(command.orderNumber());

        var result = command.fail(order);
        result.pullDomainEventIfPresent()
                .ifPresent(eventPublisher::publishEvent);
        return orderRepository.save(result);
    }

    private Order getOrderByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));
    }
}
