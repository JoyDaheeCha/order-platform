package com.flab.orderplatform.order.domain;

import com.flab.orderplatform.order.domain.status.OrderStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

import static com.flab.orderplatform.order.domain.status.OrderStatus.PENDING;

/**
 * 주문
 *
 * @param customerId  구매자 pk
 * @param orderNumber 주문번호
 * @param orderItems  주문 상품 목록
 * @param totalAmount 총 구매액
 * @param orderedAt   구매일시
 * @param status      주문 상태
 */
@Builder
public record Order(
        Long customerId,
        String orderNumber,
        List<OrderItem> orderItems,
        Long totalAmount,
        LocalDateTime orderedAt,
        OrderStatus status
) {
    /**
     * 주문한 상품
     *
     * @param productId 상품 pk
     * @param quantity  주문 수량
     */
    @Builder
    public record OrderItem(
            Long productId,
            Integer quantity
    ) {
    }

    public static Order create(Long customerId,
                               List<OrderItem> orderItems,
                               String orderNumber) {
        return Order.builder()
                .customerId(customerId)
                .orderNumber(orderNumber)
                .orderItems(orderItems)
                .orderedAt(LocalDateTime.now()) // TODO 테스트 가능한 형태로 변경 (orderedAt 주입 받도록)
                .status(PENDING)
                .build();
    }
}
