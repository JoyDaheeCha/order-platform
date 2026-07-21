package com.flab.orderplatform.order.domain;

import lombok.Builder;

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
