package com.flab.orderplatform.order.domain;

import com.flab.orderplatform.order.domain.event.OrderCreatedEvent;
import com.flab.orderplatform.order.domain.event.OrderFailedEvent;
import com.flab.orderplatform.order.domain.event.OrderPaidEvent;
import com.flab.orderplatform.order.domain.event.OrderPaymentPreparedEvent;
import com.flab.orderplatform.order.domain.status.OrderFailedReasonType;
import com.flab.orderplatform.order.domain.status.OrderInventoryReservationReleaseReason;
import com.flab.orderplatform.order.domain.status.OrderStatus;
import com.flab.orderplatform.shared.domain.BaseEntity;
import com.flab.orderplatform.shared.domain.DomainEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.flab.orderplatform.order.domain.status.OrderFailedReasonType.INVENTORY_SHORTAGE;
import static com.flab.orderplatform.order.domain.status.OrderStatus.*;
import static jakarta.persistence.CascadeType.*;
import static jakarta.persistence.FetchType.LAZY;
import static java.time.LocalDateTime.now;

/**
 * Order 컨텍스트의 영속화 모델
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", length = 36, nullable = false, unique = true,
            columnDefinition = "VARCHAR(36)  NOT NULL COMMENT '주문번호 (대외 노출용 비즈니스 키)'")
    private String orderNumber;

    @Column(name = "total_amount", nullable = false, columnDefinition = "BIGINT NOT NULL COMMENT '총 구매 금액'")
    private Long totalAmount;

    @Column(name = "ordered_at", nullable = false, columnDefinition = "DATETIME(6) NOT NULL COMMENT '주문일자'")
    private LocalDateTime orderedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false, columnDefinition = "VARCHAR(20)  NOT NULL COMMENT '주문 상태 (RESERVING_INVENTORY/PENDING/PAID/CONFIRMED/CANCELLED)'")
    private OrderStatus status;

    @OneToMany(mappedBy = "order", fetch = LAZY, cascade = {PERSIST, REMOVE, MERGE})
    private List<OrderItem> orderItems = new ArrayList<>();

    @Column(name = "customer_id", nullable = false, columnDefinition = "BIGINT NOT NULL COMMENT '구매자 ID'")
    private Long customerId;

    /**
     * 주문 생성시, 동일한 멱등키로 온 요청은 한번만 수행되도록 DB에서 방어합니다.
     */
    @Column(name = "idempotent_key", length = 36, nullable = false, unique = true,
            columnDefinition = "VARCHAR(36)  NOT NULL COMMENT '주문 생성 멱등키'")
    private String idempotentKey;

    @Transient
    private DomainEvent domainEvent;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_failed_reason", length = 30, columnDefinition = "VARCHAR(30) COMMENT '주문 실패 사유'")
    private OrderFailedReasonType reason;

    @Embedded
    private OrderInventoryReservation inventoryReservation;

    @Builder
    public Order(String orderNumber, Long totalAmount, LocalDateTime orderedAt, OrderStatus status,
                 List<OrderItem> orderItems, Long customerId, String idempotentKey,
                 OrderInventoryReservation inventoryReservation) {
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.orderedAt = orderedAt;
        this.status = status;
        this.orderItems = orderItems;
        this.customerId = customerId;
        this.idempotentKey = idempotentKey;
        this.inventoryReservation = inventoryReservation;
    }

    public static Order create(Long customerId,
                               List<OrderItem> orderItems,
                               String orderNumber,
                               String idempotentKey) {

        var orderItemDtos = orderItems
                .stream()
                .map(item -> OrderCreatedEvent.OrderItemDto
                        .builder()
                        .productCode(item.getProductCode())
                        .quantity(item.getQuantity())
                        .build())
                .toList();

        var totalAmount = orderItems.stream()
                .mapToLong(OrderItem::calculateAmount)
                .sum();

        var order = Order.builder()
                .customerId(customerId)
                .orderNumber(orderNumber)
                .orderItems(orderItems)
                .orderedAt(LocalDateTime.now())
                .status(RESERVING_INVENTORY)
                .totalAmount(totalAmount)
                .idempotentKey(idempotentKey)
                .inventoryReservation(null)
                .build();

        order.addOrderItems(orderItems);
        order.domainEvent = OrderCreatedEvent.builder()
                .orderNumber(orderNumber)
                .orderItems(orderItemDtos)
                .aggregateId(orderNumber)
                .occurredOn(now())
                .build();
        return order;
    }


    private void addOrderItems(List<OrderItem> items) {
        items.forEach(item -> item.setOrder(this));
        this.orderItems = items;
    }

    public Optional<DomainEvent> pullDomainEventIfPresent() {
        var event = Optional.ofNullable(domainEvent);
        domainEvent = null;
        return event;
    }

    public Order pay(Map<Long, String> productMapCodeById) {
        // 이미 결제 완료된 주문은 재처리하지 않는다.
        if (this.status == PAID) {
            return this;
        }
        if (this.status != PENDING_PAYMENT) {
            // TODO: 도메인 내 상태값이 유효하지 않아 예외 발생시, retryable 과 nonRetryable 로 분리후, 예외 재처리 자동화할것.
            throw new IllegalStateException("결제 대기 상태만 결제 완료 처리 가능합니다. (현재상태: %s)".formatted(status));
        }
        this.status = PAID;
        this.domainEvent = OrderPaidEvent.builder()
                .orderNumber(orderNumber)
                .orderItems(convertToOrderItems(productMapCodeById))
                .aggregateId(orderNumber)
                .occurredOn(now())
                .build();
        return this;
    }

    private List<OrderPaidEvent.OrderItemDto> convertToOrderItems(Map<Long, String> productMapCodeById) {
        return this.orderItems.stream()
                .map(item -> {
                    var productCode = getProductCode(productMapCodeById, item);
                    return OrderPaidEvent
                            .OrderItemDto
                            .builder()
                            .productCode(productCode)
                            .quantity(item.getQuantity())
                            .build();
                }).toList();
    }

    private String getProductCode(Map<Long, String> productMapCodeById, OrderItem item) {
        return productMapCodeById.get(item.getProductId());
    }

    /**
     * 결제 대기<br>
     * 주문에서 재고 선점 완료후, 결제 대기로 넘어간다
     *
     * @param reservedAt 재고 선점일시
     */
    public Order preparePayment(LocalDateTime reservedAt) {
        // 재고 선점중일때만 처리
        if (status != RESERVING_INVENTORY) {
            throw new IllegalStateException("결제 대기 진입은 %s 상태에서만 가능합니다. (현재 주문 상태: %s)"
                    .formatted(RESERVING_INVENTORY.getDescription(), this.status.getDescription()));
        }
        this.inventoryReservation = OrderInventoryReservation.create(reservedAt);
        this.status = PENDING_PAYMENT;
        this.domainEvent = OrderPaymentPreparedEvent.builder()
                .orderNumber(orderNumber)
                .aggregateId(orderNumber)
                .amount(totalAmount)
                .buyerId(customerId)
                .occurredOn(now())
                .build();
        return this;
    }

    /**
     * 재고 부족으로 인한 주문 실패 처리<br>
     * 재고 선점에 실패하였으므로, 별도 이벤트 발행 없음
     */
    public Order failByInventoryShortage() {
        // 재고 선점중일때만 처리
        if (this.status != RESERVING_INVENTORY) {
            throw new IllegalStateException("재고부족으로 인한 주문 실패 처리는 %s 상태에서만 가능합니다. (현재 주문 상태: %s)"
                    .formatted(RESERVING_INVENTORY.getDescription(), this.status.getDescription()));
        }
        this.status = ORDER_FAILED;
        this.reason = INVENTORY_SHORTAGE;
        return this;
    }

    /**
     * 재고 선점후 일정 시간 내 결제가 이뤄지지 않아 주문 실패 처리
     */
    public Order failByTimeout() {
        // 이미 결제 완료/실패한 주문은 무시
        if (this.status != PENDING_PAYMENT) {
            throw new IllegalStateException("결제 타임 아웃으로 인한 주문 실패 처리는 %s 상태에서만 가능합니다. (현재 주문 상태: %s)"
                    .formatted(RESERVING_INVENTORY.getDescription(), this.status.getDescription()));
        }
        this.status = ORDER_FAILED;
        this.reason = OrderFailedReasonType.TIMEOUT;
        this.inventoryReservation = inventoryReservation.release(OrderInventoryReservationReleaseReason.TIMEOUT);

        this.domainEvent = OrderFailedEvent.builder()
                .orderNumber(orderNumber)
                .aggregateId(orderNumber)
                .occurredOn(now())
                .orderItems(orderItems)
                .build();
        return this;
    }
}
