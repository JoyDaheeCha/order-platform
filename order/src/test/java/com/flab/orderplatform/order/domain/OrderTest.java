package com.flab.orderplatform.order.domain;

import com.flab.orderplatform.order.domain.event.OrderCreatedEvent;
import com.flab.orderplatform.order.domain.event.OrderPaidEvent;
import com.flab.orderplatform.order.domain.status.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.flab.orderplatform.order.domain.status.OrderInventoryReservationReleaseReason.PAYMENT_COMPLETED;
import static com.flab.orderplatform.order.domain.status.OrderStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

@DisplayName("주문 단위테스트")
class OrderTest {

    @DisplayName("[성공] 주문 생성시 주문일자는 현재로 세팅되고, 주문 상태는 '재고 선점중'으로 초기화된다.")
    @Test
    void create() {
        // given
        var customerId = 100L;
        var orderNumber = "20260721-7K3M9QX2WF";
        var orderItems = List.of(
                OrderItem.builder()
                        .productId(1L)
                        .productCode("GD10001")
                        .name("뽀로로 주스")
                        .price(1_500L)
                        .quantity(3)
                        .build()
        );
        Order order;
        var fixedNow = LocalDateTime.of(2026, 7, 21, 14, 30, 0);
        try (MockedStatic<LocalDateTime> mocked = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            mocked.when(LocalDateTime::now).thenReturn(fixedNow);
            // when
            order = Order.create(customerId, orderItems, orderNumber, UUID.randomUUID().toString());
        }

        // then
        assertSoftly(softly -> {
            softly.assertThat(order.getOrderedAt()).isEqualTo(fixedNow);
            softly.assertThat(order.getStatus()).isEqualTo(RESERVING_INVENTORY);
        });
    }

    @DisplayName("[성공] 주문 생성시 총 구매금액은 주문상품 금액의 합으로 계산된다.")
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
        var order = Order.create(100L, orderItems, orderNumber, UUID.randomUUID().toString());

        // then
        assertSoftly(softly -> {
            softly.assertThat(order.getOrderNumber()).isEqualTo(orderNumber);
            softly.assertThat(order.getTotalAmount()).isEqualTo(8_500L);
            softly.assertThat(order.getOrderItems()).hasSize(2);
        });
    }

    @DisplayName("[성공] 주문 생성시 전달한 멱등키가 그대로 보존된다.")
    @Test
    void createKeepsIdempotentKey() {
        // given
        var idempotentKey = "1111-2222-3333-4444";

        // when
        var order = Order.create(100L, List.of(orderItem()), "20260721-2P8HNRT4VZ", idempotentKey);

        // then: 멱등키는 DB unique 제약으로 중복 주문을 막는 값이므로 변형 없이 그대로 실려야 한다.
        assertThat(order.getIdempotentKey()).isEqualTo(idempotentKey);
    }

    @DisplayName("[성공] 주문을 결제하면 (1) 결제완료로 변경, (2) 선점된 재고가 해제, (3) 결제완료 이벤트가 등록된다.")
    @Test
    void payTransitionsToPaidAndRegistersEvent() {
        // given: 생성 시점의 OrderCreatedEvent 는 이미 발행되었다고 보고 비워둔다.
        var  createdOrder  = Order.builder()
                .customerId(100L)
                .orderItems(List.of(orderItem()))
                .status(RESERVING_INVENTORY)
                .orderNumber("20260730-5T1QWE9BXK")
                .idempotentKey("1111-2222-3333-4444")
                .build();
        var order = createdOrder.preparePayment(LocalDateTime.now());
        order.pullDomainEventIfPresent();

        // when
        var result = order.pay(Map.of(1L, "GD10001"));

        // then
        var event = (OrderPaidEvent) order.pullDomainEventIfPresent().orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(result).isSameAs(order);
            softly.assertThat(order.getStatus()).isEqualTo(PAID);
            softly.assertThat(order.getInventoryReservation().getIsReleased()).isTrue();
            softly.assertThat(order.getInventoryReservation().getReleaseReason()).isEqualTo(PAYMENT_COMPLETED);
            softly.assertThat(event.getOrderNumber()).isEqualTo("20260730-5T1QWE9BXK");
            softly.assertThat(event.getAggregateId()).isEqualTo("20260730-5T1QWE9BXK");
            // 재고 차감은 productId 가 아니라 productCode 로 이뤄지므로, 여기서 코드로 치환된다.
            softly.assertThat(event.getOrderItems())
                    .containsExactly(new OrderPaidEvent.OrderItemDto("GD10001", 3));
        });
    }

    @DisplayName("[성공] 이미 결제완료된 주문을 다시 결제하면 이벤트를 새로 등록하지 않는다.")
    @Test
    void payOnAlreadyPaidOrderIsIdempotent() {
        // given: 인박스가 같은 결제완료 메시지를 재처리하는 상황
        var order = orderWith(PAID);

        // when
        var result = order.pay(Map.of(1L, "GD10001"));

        // then: 예외 없이 통과하되, 이벤트를 다시 쌓지 않아야 재고 차감이 두 번 일어나지 않는다.
        assertSoftly(softly -> {
            softly.assertThat(result).isSameAs(order);
            softly.assertThat(order.getStatus()).isEqualTo(PAID);
            softly.assertThat(order.pullDomainEventIfPresent()).isEmpty();
        });
    }

    @DisplayName("[실패] 결제대기·결제완료가 아닌 주문은 결제 완료 처리할 수 없다.")
    @ParameterizedTest(name = "[실패] 결제대기·결제완료가 아닌 주문은 결제 완료 처리할 수 없다. (현재상태={0})")
    @EnumSource(value = OrderStatus.class, names = {"CONFIRMED", "CANCELLED"})
    void payFailsOnUnexpectedStatus(OrderStatus status) {
        // given
        var order = orderWith(status);

        // when & then
        assertThatThrownBy(() -> order.pay(Map.of(1L, "GD10001")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(status.name());
    }

    @DisplayName("[성공] 주문 생성시 주문 생성 이벤트가 애그리거트에 등록된다.")
    @Test
    void createRegistersDomainEvent() {
        // given
        var orderNumber = "20260730-5T1QWE9BXK";
        var orderItems = List.of(
                OrderItem.builder()
                        .productId(1L)
                        .name("뽀로로 주스")
                        .productCode("GD10001")
                        .price(1_500L)
                        .quantity(3)
                        .build()
        );

        // when
        var order = Order.create(100L, orderItems, orderNumber, "1111-2222-3333-4444");

        // then
        var event = (OrderCreatedEvent) order.pullDomainEventIfPresent().get();
        assertSoftly(softly -> {
            softly.assertThat(event).isNotNull();
            softly.assertThat(event.getAggregateId()).isEqualTo(orderNumber);
            softly.assertThat(event.getEventId()).isNotBlank();
            softly.assertThat(event.getOrderItems())
                    .containsExactly(new OrderCreatedEvent.OrderItemDto("GD10001", 3));
        });
    }

    @DisplayName("[성공] 등록된 이벤트는 한 번만 꺼내진다. (중복 발행 방지)")
    @Test
    void pullDomainEventDrainsTheEventIfPresent() {
        // given
        var order = Order.create(100L, List.of(
                        OrderItem.builder()
                                .productId(1L)
                                .productCode("GD10001")
                                .name("뽀로로 주스")
                                .price(1_500L)
                                .quantity(3)
                                .build()
                ), "20260730-8N4ZLC2RPD",
                "1111-2222-3333-4444");

        // when: 첫 번째 pull 로 이벤트를 꺼낸다.
        var firstPull = order.pullDomainEventIfPresent();

        // then: 두 번째 pull 은 비어 있어야 한다.
        //       비어 있지 않으면 같은 이벤트가 outbox 에 두 번 쌓이고 Kafka 로도 두 번 나간다.
        assertSoftly(softly -> {
            softly.assertThat(firstPull).isPresent();
            softly.assertThat(order.pullDomainEventIfPresent()).isEmpty();
        });
    }

    @DisplayName("[성공] 등록된 이벤트가 없는 주문에서 이벤트를 꺼내면 비어 있다.")
    @Test
    void pullDomainEventReturnsEmptyWhenNothingRegistered() {
        // given: DB 에서 조회해 온 주문처럼 domainEvent 가 비어 있는 상태 (@Transient 라 영속화되지 않는다)
        var order = orderWith(PENDING_PAYMENT);

        // when & then
        assertThat(order.pullDomainEventIfPresent()).isEmpty();
    }

    private OrderItem orderItem() {
        return OrderItem.builder()
                .productId(1L)
                .name("뽀로로 주스")
                .price(1_500L)
                .quantity(3)
                .build();
    }

    /**
     * 팩토리(create)를 거치지 않고 특정 상태의 주문을 만든다. 조회해 온 주문을 흉내내는 용도.
     */
    private Order orderWith(OrderStatus status) {
        var orderItems = List.of(orderItem());
        var order = Order.builder()
                .orderNumber("20260730-8N4ZLC2RPD")
                .totalAmount(4_500L)
                .orderedAt(LocalDateTime.of(2026, 7, 30, 10, 0))
                .status(status)
                .orderItems(orderItems)
                .customerId(100L)
                .idempotentKey("1111-2222-3333-4444")
                .build();
        orderItems.forEach(item -> item.setOrder(order));
        return order;
    }

    @DisplayName("결제 대기시, 재고가 선점되고 '결제대기' 상태로 변경된다.")
    @Test
    void preparePayment() {
        // given
        var order = orderWith(RESERVING_INVENTORY);
        var reservedAt = LocalDateTime.of(2026, 7, 21, 14, 30, 0);

        // when
        order.preparePayment(reservedAt);

        assertSoftly(softly -> {
                    softly.assertThat(order.getInventoryReservation().getReservedAt()).isEqualTo(reservedAt);
                    softly.assertThat(order.getInventoryReservation().getIsReleased()).isFalse();
                    softly.assertThat(order.getStatus()).isEqualTo(PENDING_PAYMENT);
                }
        );
    }
}
