package com.flab.orderplatform.order.domain;

import lombok.Builder;

/**
 * 주문한 상품
 *
 * @param productId 상품 pk
 * @param name 상품명 스냅샷
 * @param price 상품가격
 * @param productCode 상품 코드
 * @param quantity  주문 수량
 */
@Builder
public record OrderItem(
        Long productId,
        String name,
        Long price,
        String productCode,
        Integer quantity
) {
}
