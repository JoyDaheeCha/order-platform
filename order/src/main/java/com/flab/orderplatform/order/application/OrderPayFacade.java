package com.flab.orderplatform.order.application;

import com.flab.orderplatform.order.application.command.OrderPayCommand;
import com.flab.orderplatform.order.application.command.OrderPreparePaymentCommand;
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

    /**
     * 주문 결제 완료
     *
     * @param orderNumber 주문번호
     * @return 주문
     */
    public Order pay(String orderNumber) {
        var order = getOrder(orderNumber);
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

    private Order getOrder(String orderNumber) {
        return orderRepository.findWithOrderItemsByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));
    }

    /**
     * 재고 선점 & 결제 준비 완료
     *
     * @param orderNumber 주문번호
     */
    public Order preparePayment(String orderNumber) {
        // TODO 재고 선점 스케줄러 데이터 등록
        var command = new OrderPreparePaymentCommand(orderNumber);
        return orderCommandHandler.handle(command);
    }
}
