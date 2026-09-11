package com.flab.orderplatform.order.application;

import com.flab.orderplatform.order.application.command.OrderCreateCommand;
import com.flab.orderplatform.order.application.command.OrderFailByPaymentTimeoutCommand;
import com.flab.orderplatform.order.application.command.OrderPayCommand;
import com.flab.orderplatform.order.application.command.OrderPreparePaymentCommand;
import com.flab.orderplatform.order.application.exception.DuplicatedProductException;
import com.flab.orderplatform.order.application.exception.OrderNotFoundException;
import com.flab.orderplatform.order.application.exception.ProductNotFoundException;
import com.flab.orderplatform.order.application.port.out.OrderRepository;
import com.flab.orderplatform.order.domain.Order;
import com.flab.orderplatform.order.domain.OrderItem;
import com.flab.orderplatform.order.domain.event.OrderCreatedEvent;
import com.flab.orderplatform.order.domain.event.OrderFailedEvent;
import com.flab.orderplatform.order.domain.event.OrderPaidEvent;
import com.flab.orderplatform.order.domain.event.OrderPaymentPreparedEvent;
import com.flab.orderplatform.order.domain.external.Product;
import com.flab.orderplatform.order.domain.status.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.flab.orderplatform.order.domain.status.OrderStatus.*;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("주문 커멘드 핸들러 테스트")
class OrderCommandHandlerTest {

    private static final String ORDER_NUMBER = "20260730-8N4ZLC2RPD";
    private static final String IDEMPOTENT_KEY = "1111-2222-3333-4444";

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @InjectMocks
    private OrderCommandHandler orderCommandHandler;

    private OrderCreateCommand createCommand(OrderCreateCommand.OrderItemDto... orderItems) {
        return OrderCreateCommand.builder()
                .customerId(1L)
                .orderItems(List.of(orderItems))
                .idempotentKey(IDEMPOTENT_KEY)
                .build();
    }

    /** 저장소는 넘겨받은 애그리거트를 그대로 돌려준다고 본다. (ID 채번 외 부수효과 없음) */
    private void givenSaveReturnsGivenOrder() {
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));
    }

    private Order orderWith(OrderStatus status) {
        var orderItems = List.of(
                OrderItem.builder()
                        .productId(1L)
                        .name("뽀로로주스")
                        .price(1_500L)
                        .quantity(3)
                        .build()
        );
        var order = Order.builder()
                .orderNumber(ORDER_NUMBER)
                .totalAmount(4_500L)
                .orderedAt(LocalDateTime.of(2026, 7, 30, 10, 0))
                .status(status)
                .orderItems(orderItems)
                .customerId(1L)
                .idempotentKey(IDEMPOTENT_KEY)
                .build();
        orderItems.forEach(item -> item.setOrder(order));
        return order;
    }

    @Nested
    @DisplayName("주문 생성")
    class CreateOrder {

        @Test
        @DisplayName("[성공] 주문 생성시 주문상품의 상품ID와 단가는 실시간 상품 정보로 채워진다.")
        void fillsProductIdAndPriceFromProductMap() {
            // given: 단가는 클라이언트가 보낸 값이 아니라 조회한 상품 정보를 신뢰한다.
            var command = createCommand(
                    new OrderCreateCommand.OrderItemDto(2, "뽀로로주스사과맛", "GD10001"),
                    new OrderCreateCommand.OrderItemDto(3, "뽀로로짜장면", "GD10002")
            );
            var productMap = Map.of(
                    "GD10001", new Product(1L, "GD10001", 1_000L),
                    "GD10002", new Product(2L, "GD10002", 2_000L)
            );
            givenSaveReturnsGivenOrder();

            // when
            var order = orderCommandHandler.handle(ORDER_NUMBER, productMap, command);

            // then
            assertSoftly(softly -> {
                softly.assertThat(order.getOrderItems()).hasSize(2);
                softly.assertThat(order.getOrderItems())
                        .extracting(OrderItem::getProductId, OrderItem::getPrice, OrderItem::getQuantity)
                        .containsExactly(
                                tuple(1L, 1_000L, 2),
                                tuple(2L, 2_000L, 3)
                        );
                // 총 구매금액도 조회한 단가 기준으로 계산된다. (1,000 × 2) + (2,000 × 3)
                softly.assertThat(order.getTotalAmount()).isEqualTo(8_000L);
            });
        }

        @Test
        @DisplayName("[성공] 주문상품명은 조회한 상품이 아니라 주문 요청의 스냅샷을 사용한다.")
        void keepsRequestedProductNameAsSnapshot() {
            // given: Product 에는 상품명이 없다. 주문 시점의 상품명은 요청값을 그대로 박제한다.
            var command = createCommand(new OrderCreateCommand.OrderItemDto(2, "뽀로로주스", "GD10001"));
            var productMap = Map.of("GD10001", new Product(1L, "GD10001", 1_000L));
            givenSaveReturnsGivenOrder();

            // when
            var order = orderCommandHandler.handle(ORDER_NUMBER, productMap, command);

            // then
            assertThat(order.getOrderItems())
                    .extracting(OrderItem::getName)
                    .containsExactly("뽀로로주스");
        }

        @Test
        @DisplayName("[성공] 주문이 저장되면 주문 생성 이벤트가 발행된다.")
        void publishesOrderCreatedEvent() {
            // given
            var command = createCommand(new OrderCreateCommand.OrderItemDto(2, "뽀로로주스", "GD10001"));
            var productMap = Map.of("GD10001", new Product(1L, "GD10001", 1_000L));
            givenSaveReturnsGivenOrder();

            // when
            orderCommandHandler.handle(ORDER_NUMBER, productMap, command);

            // then: 이벤트 발행이 빠지면 outbox 에 적재되지 않아 결제/재고로 흐르지 않는다.
            var captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
            verify(eventPublisher, times(1)).publishEvent(captor.capture());
            assertThat(captor.getValue().getAggregateId()).isEqualTo(ORDER_NUMBER);
        }

        @Test
        @DisplayName("[실패] 상품 맵에 없는 상품코드가 있으면 주문을 생성하지 않는다")
        void failsWhenProductCodeIsMissingFromMap() {
            // given: GD10002 가 상품 맵에 없다.
            var command = createCommand(
                    new OrderCreateCommand.OrderItemDto(2, "뽀로로주스", "GD10001"),
                    new OrderCreateCommand.OrderItemDto(3, "뽀로로짜장면", "GD10002")
            );
            var productMap = Map.of("GD10001", new Product(1L, "GD10001", 1_000L));

            // when & then
            assertThatThrownBy(() -> orderCommandHandler.handle(ORDER_NUMBER, productMap, command))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessageContaining("GD10002");

            // 일부만 담긴 주문이 저장되지 않아야 한다.
            verify(orderRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any(OrderCreatedEvent.class));
        }

        @Test
        @DisplayName("[실패] 상품코드를 중복하여 주문할 수 없다.")
        void failsWhenProductCodeIsDuplicated() {
            // given: 같은 상품이 주문에 존재
            var command = createCommand(
                    new OrderCreateCommand.OrderItemDto(2, "뽀로로주스사과맛", "GD10001"),
                    new OrderCreateCommand.OrderItemDto(3, "뽀로로주스사과맛", "GD10001")
            );
            var productMap = Map.of("GD10001", new Product(1L, "GD10001", 1_000L));

            // when & then: 메시지에는 중복된 코드만 담긴다.
            assertThatThrownBy(() -> orderCommandHandler.handle(ORDER_NUMBER, productMap, command))
                    .isInstanceOf(DuplicatedProductException.class)
                    .hasMessageContaining("GD10001");

            verify(orderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("주문 결제 완료 처리")
    class PayOrder {

        @Test
        @DisplayName("[성공] 결제 완료 처리시 주문이 결제완료로 바뀌고 결제완료 이벤트가 발행된다.")
        void marksOrderAsPaidAndPublishesEvent() {
            // given
            var order = orderWith(PENDING_PAYMENT);
            given(orderRepository.findWithOrderItemsByOrderNumber(ORDER_NUMBER))
                    .willReturn(Optional.of(order));
            givenSaveReturnsGivenOrder();

            // when
            var result = orderCommandHandler.handle(payCommand());

            // then
            var captor = ArgumentCaptor.forClass(OrderPaidEvent.class);
            verify(eventPublisher, times(1)).publishEvent(captor.capture());
            verify(orderRepository, times(1)).save(order);
            assertSoftly(softly -> {
                softly.assertThat(result.getStatus()).isEqualTo(PAID);
                softly.assertThat(captor.getValue().getOrderNumber()).isEqualTo(ORDER_NUMBER);
                softly.assertThat(captor.getValue().getOrderItems())
                        .containsExactly(new OrderPaidEvent.OrderItemDto("GD10001", 3));
            });
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 주문번호는 결제 완료 처리할 수 없다.")
        void failsWhenOrderDoesNotExist() {
            // given
            given(orderRepository.findWithOrderItemsByOrderNumber(ORDER_NUMBER))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderCommandHandler.handle(payCommand()))
                    .isInstanceOf(OrderNotFoundException.class)
                    .hasMessageContaining(ORDER_NUMBER);

            verify(orderRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any(OrderPaidEvent.class));
        }

        @Test
        @DisplayName("[성공] 이미 결제완료된 주문이 다시 들어와도 이벤트를 재발행하지 않는다.")
        void doesNotRepublishEventForAlreadyPaidOrder() {
            // given: 인박스가 같은 PaymentCompleted 메시지를 재처리하는 상황
            var order = orderWith(PAID);
            given(orderRepository.findWithOrderItemsByOrderNumber(ORDER_NUMBER))
                    .willReturn(Optional.of(order));
            givenSaveReturnsGivenOrder();

            // when
            var result = orderCommandHandler.handle(payCommand());

            // then: 이벤트 재발행되지 않는다.
            verify(eventPublisher, never()).publishEvent(any(OrderPaidEvent.class));
            assertThat(result.getStatus()).isEqualTo(PAID);
        }

        private OrderPayCommand payCommand() {
            return OrderPayCommand.builder()
                    .orderNumber(ORDER_NUMBER)
                    .productMapCodeById(Map.of(1L, "GD10001"))
                    .build();
        }
    }

    @DisplayName("결제 준비시, '결제 준비완료' 메시지가 발행된다.")
    @Test
    void preparePayment() {
        // given
        var order = orderWith(RESERVING_INVENTORY);
        var command = new OrderPreparePaymentCommand(ORDER_NUMBER, LocalDateTime.now());
        given(orderRepository.findByOrderNumber(ORDER_NUMBER))
                .willReturn(Optional.of(order));

        // when
        orderCommandHandler.handle(command);

        // then
        var captor = ArgumentCaptor.forClass(OrderPaymentPreparedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
    }

    @DisplayName("타임아웃으로 주문실패시, '주문 실패' 메시지가 발행된다.")
    @Test
    void failByPaymentTimeout() {
        // given
        var order = orderWith(RESERVING_INVENTORY)
                .preparePayment(LocalDateTime.now());

        var command = new OrderFailByPaymentTimeoutCommand(ORDER_NUMBER);
        given(orderRepository.findByOrderNumber(ORDER_NUMBER))
                .willReturn(Optional.of(order));

        // when
        orderCommandHandler.handle(command);

        // then
        var captor = ArgumentCaptor.forClass(OrderFailedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
    }
}
