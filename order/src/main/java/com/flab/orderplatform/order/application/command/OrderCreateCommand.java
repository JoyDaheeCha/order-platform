package com.flab.orderplatform.order.application.command;

import com.flab.orderplatform.order.domain.Order;
import com.flab.orderplatform.order.domain.OrderItem;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 주문 생성
 *
 */
public final class OrderCreateCommand extends IdempotentKeyCommand {
    private final Long customerId;
    @Getter
    private final List<OrderItemDto> orderItems;

    /**
     * @param idempotentKey 멱등키
     * @param customerId    구매자 PK
     * @param orderItems    주문 상품 목록
     */
    @Builder
    public OrderCreateCommand(
            String idempotentKey,
            Long customerId,
            List<OrderItemDto> orderItems
    ) {
        this.idempotentKey = idempotentKey;
        this.customerId = customerId;
        this.orderItems = orderItems;
    }

    /**
     * 주문한 상품
     *
     * @param quantity    주문 수량
     * @param name        상품명
     * @param productCode 상품 코드
     */
    @Builder
    public record OrderItemDto(
            Integer quantity,
            String name,
            String productCode
    ) {
    }

    public Order createOrder(String orderNumber, List<OrderItem> orderItems) {
        return Order.create(customerId, orderItems, orderNumber, idempotentKey);
    }

    public List<String> getProductCodes() {
        return this.orderItems.stream()
                .map(OrderItemDto::productCode)
                .toList();
    }
}
