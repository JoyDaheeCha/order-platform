package com.flab.orderplatform.order.domain;

import com.flab.orderplatform.order.domain.status.OrderStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.flab.orderplatform.order.domain.status.OrderStatus.PENDING;
import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.CascadeType.REMOVE;

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
    @Column(length = 20, nullable = false, columnDefinition = "VARCHAR(20)  NOT NULL COMMENT '주문 상태 (PENDING/PAID/CONFIRMED/CANCELLED)'")
    private OrderStatus status;

    @OneToMany(cascade = {PERSIST, REMOVE})
    @JoinColumn(name = "order_id", nullable = false)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Column(name = "customer_id", nullable = false, columnDefinition = "BIGINT NOT NULL COMMENT '구매자 ID'")
    private Long customerId;

    /**
     * 주문 생성시, 동일한 멱등키로 온 요청은 한번만 수행되도록 DB에서 방어합니다.
     */
    @Column(name = "idempotent_key", length = 36, nullable = false, unique = true,
            columnDefinition = "VARCHAR(36)  NOT NULL COMMENT '주문 생성 멱등키'")
    private String idempotentKey;

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

        var totalAmount = orderItems.stream()
                .mapToLong(OrderItem::calculateAmount)
                .sum();

        return Order.builder()
                .customerId(customerId)
                .orderNumber(orderNumber)
                .orderItems(orderItems)
                .orderedAt(LocalDateTime.now())
                .status(PENDING)
                .totalAmount(totalAmount)
                .idempotentKey(idempotentKey)
                .build();
    }
}