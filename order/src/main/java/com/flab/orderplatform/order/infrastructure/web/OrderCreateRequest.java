package com.flab.orderplatform.order.infrastructure.web;

import com.flab.orderplatform.order.domain.Order;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 주문 생성
 *
 * @param customerId    구매자 PK
 * @param orderItemDtos 주문 상품 목록
 */
public record OrderCreateRequest(
        @NotNull
        Long customerId,
        @NotNull
        List<OrderItemDto> orderItemDtos
) {
    /**
     * 주문한 상품
     *
     * @param productId 상품 pk
     * @param quantity  주문 수량
     */
    public record OrderItemDto(
            @NotNull
            Long productId,
            @NotNull
            Integer quantity
    ) {
    }

    public List<Order.OrderItem> toOrderItems() {
        return orderItemDtos.stream()
                .map(dto -> Order.OrderItem.builder()
                        .productId(dto.productId)
                        .quantity(dto.quantity)
                        .build())
                .toList();
    }
}
