package com.flab.orderplatform.order.application;

import com.flab.orderplatform.order.application.command.*;
import com.flab.orderplatform.order.application.exception.OrderNotFoundException;
import com.flab.orderplatform.order.application.port.out.OrderRepository;
import com.flab.orderplatform.order.application.port.out.ProductRepository;
import com.flab.orderplatform.order.domain.Order;
import com.flab.orderplatform.order.domain.OrderItem;
import com.flab.orderplatform.order.domain.external.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import static com.flab.orderplatform.order.domain.status.OrderStatus.PENDING_PAYMENT;
import static java.time.LocalDateTime.now;

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
     * @return 주문
     */
    public Order preparePayment(String orderNumber, LocalDateTime reservedAt) {
        var command = new OrderPreparePaymentCommand(orderNumber, reservedAt);
        return orderCommandHandler.handle(command);
    }

    /**
     * 재고 부족으로 인한 주문 실패 처리
     *
     * @param orderNumber 주문번호
     * @return 주문
     */
    public Order failOrderByInventoryShortage(String orderNumber) {
        return orderCommandHandler.handle(new OrderFailByInventoryShortageCommand(orderNumber));
    }

    /**
     * 재고 선점된지 10분이 지난 데이터 일괄 해제
     */
    public void releaseReservation() {
        var orderNumbers = orderRepository.findReleaseTarget(now().minusMinutes(10), PENDING_PAYMENT)
                .stream().map(Order::getOrderNumber)
                .toList();

        orderNumbers
                .forEach(orderNumber -> orderCommandHandler.handle(new OrderFailByPaymentTimeoutCommand(orderNumber)));
    }

    /**
     * 결제 실패로 인한 주문 실패 처리
     *
     * @param orderNumber 주문 번호
     */
    public Order failOrderByFailedPayment(String orderNumber) {
        return orderCommandHandler.handle(new OrderFailByPaymentFailCommand(orderNumber));
    }
}
