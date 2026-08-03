package com.flab.orderplatform.order.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("주문상품 단위테스트")
class OrderItemTest {

    @DisplayName("[성공] 주문상품 금액은 단가 × 수량으로 계산된다.")
    @Test
    void calculateAmount() {
        // given
        var orderItem = OrderItem.builder()
                .productId(1L)
                .name("뽀로로 주스")
                .price(1_500L)
                .quantity(3)
                .build();

        // when
        var amount = orderItem.calculateAmount();

        // then
        assertThat(amount).isEqualTo(4_500L);
    }
}
