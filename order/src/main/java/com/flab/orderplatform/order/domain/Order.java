package com.flab.orderplatform.order.domain;

import com.flab.orderplatform.order.domain.event.OrderCreatedEvent;
import com.flab.orderplatform.order.domain.event.OrderPaidEvent;
import com.flab.orderplatform.order.domain.event.OrderPaymentPreparedEvent;
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
    @Column(length = 20, nullable = false, columnDefinition = "VARCHAR(20)  NOT NULL COMMENT '주문 상태 (RESERVING_INVENTORY/PENDING/PAID/CONFIRMED/CANCELLED)'")
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

    @OneToOne(fetch = LAZY, cascade = {PERSIST, REMOVE, MERGE}, orphanRemoval = true)
    @JoinColumn(name = "order_failed_reason_id")
    private OrderFailedReason orderFailedReason;

    @OneToOne(fetch = LAZY, cascade = {PERSIST, REMOVE, MERGE}, orphanRemoval = true)
    @JoinColumn(name = "order_inventory_reservation_id")
    private OrderInventoryReservation inventoryReservation;

    @Builder
    public Order(String orderNumber, Long totalAmount, LocalDateTime orderedAt, OrderStatus status,
                 List<OrderItem> orderItems, Long customerId, String idempotentKey) {
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.orderedAt = orderedAt;
        this.status = status;
        this.orderItems = orderItems;
        this.customerId = customerId;
        this.idempotentKey = idempotentKey;
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

        var order = Order.builder()
                .customerId(customerId)
                .orderNumber(orderNumber)
                .orderItems(orderItems)
                .orderedAt(LocalDateTime.now())
                .status(RESERVING_INVENTORY)
                .idempotentKey(idempotentKey)
                .build();

        order.addOrderItems(orderItems);
        order.domainEvent = OrderCreatedEvent.builder()
                .orderNumber(orderNumber)
                .orderItems(orderItemDtos)
                .aggregateId(orderNumber)
                .occurredOn(LocalDateTime.now())
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
            throw new IllegalStateException("결제 대기 상태만 결제 완료 처리 가능합니다. (현재상태: %s)".formatted(status));
        }
        this.status = PAID;
        this.domainEvent = OrderPaidEvent.builder()
                .orderNumber(orderNumber)
                .orderItems(convertToOrderItems(productMapCodeById))
                .aggregateId(orderNumber)
                .occurredOn(LocalDateTime.now())
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

    public Order preparePayment(LocalDateTime reservedAt) {
        this.inventoryReservation = OrderInventoryReservation.create(orderNumber, reservedAt);
        this.status = PENDING_PAYMENT;
        this.domainEvent = OrderPaymentPreparedEvent.builder()
                .orderNumber(orderNumber)
                .aggregateId(orderNumber)
                .occurredOn(LocalDateTime.now())
                .build();
        return this;
    }

    public Order failByInventoryShortage() {
        this.status = ORDER_FAILED;
        this.orderFailedReason = OrderFailedReason.create(INVENTORY_SHORTAGE);
        return this;
    }
}
