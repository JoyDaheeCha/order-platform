package com.flab.orderplatform.order.domain;

import com.flab.orderplatform.order.domain.status.OrderStatus;
import com.flab.orderplatform.order.infrastructure.persistence.OrderItemEntity;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    // TODO: 도메인 로직 테스트 추가 (주문번호 지정, 주문일자 현재, status pending이다 확인)
    public static Order create(Long customerId,
                               List<OrderItem> orderItems,
                               String orderNumber,
                               Map<String, Long> productPriceMap) {
        var orderItemsWithPrice = orderItems.stream().map(item -> OrderItem.builder()
                        .productId(item.productId())
                        .name(item.name())
                        .price(productPriceMap.get(item.productCode()))
                        .productCode(item.productCode())
                        .quantity(item.quantity())
                        .build())
                .toList();

        var totalAmount = orderItems.stream()
                .mapToLong(OrderItem::price)
                .sum();
        return Order.builder()
                .customerId(customerId)
                .orderNumber(orderNumber)
                .orderItems(orderItemsWithPrice)
                .orderedAt(LocalDateTime.now()) // TODO 테스트 가능한 형태로 변경 (orderedAt 주입 받도록)
                .status(PENDING)
                .totalAmount(totalAmount)
                .build();
    }
}
