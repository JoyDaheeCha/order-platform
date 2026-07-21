package com.flab.orderplatform.order.domain;

import lombok.Builder;

/**
 * 상품
 * @param productId 상품 PK
 * @param productCode 상품코드
 * @param price 가격
 */
@Builder
public record Product (
        Long productId,
        String productCode,
        Long price
){
}
