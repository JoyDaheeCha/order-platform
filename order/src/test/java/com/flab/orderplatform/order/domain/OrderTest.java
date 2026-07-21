package com.flab.orderplatform.order.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static com.flab.orderplatform.order.domain.status.OrderStatus.PENDING;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DisplayName("주문 단위테스트")
class OrderTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 21, 14, 30, 0);
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);

    @DisplayName("주문 생성시 주문일자는 현재로 세팅되고, 주문 상태는 결제대기중으로 초기화된다.")
    @Test
    void create() {
        // given
        var customerId = 100L;
        var orderNumber = "20260721-7K3M9QX2WF";
        var orderItems = List.of(
                OrderItem.builder()
                        .productId(1L)
                        .name("뽀로로 주스")
                        .price(1_500L)
                        .quantity(3)
                        .build()
        );

        // when
        var order = Order.create(customerId, orderItems, orderNumber, FIXED_CLOCK);

        // then
        assertSoftly(softly -> {
            softly.assertThat(order.getOrderedAt()).isEqualTo(NOW);
            softly.assertThat(order.getStatus()).isEqualTo(PENDING);
        });
    }

    @DisplayName("주문 생성시 주문번호가 지정되고, 총 구매금액은 주문상품 금액의 합으로 계산된다.")
    @Test
    void createWithTotalAmount() {
        // given
        var orderNumber = "20260721-2P8HNRT4VZ";
        var orderItems = List.of(
                OrderItem.builder()
                        .productId(1L)
                        .name("뽀로로 주스")
                        .price(1_500L)
                        .quantity(3)   // 4,500
                        .build(),
                OrderItem.builder()
                        .productId(2L)
                        .name("뽀로로 짜장면")
                        .price(2_000L)
                        .quantity(2)   // 4,000
                        .build()
        );

        // when
        var order = Order.create(100L, orderItems, orderNumber, FIXED_CLOCK);

        // then
        assertSoftly(softly -> {
            softly.assertThat(order.getOrderNumber()).isEqualTo(orderNumber);
            softly.assertThat(order.getTotalAmount()).isEqualTo(8_500L);
            softly.assertThat(order.getOrderItems()).hasSize(2);
        });
    }
}
