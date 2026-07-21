package com.flab.orderplatform.order.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.flab.orderplatform.order.domain.Order;
import com.flab.orderplatform.order.domain.status.OrderStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.CascadeType.REMOVE;

/**
 * Order 컨텍스트의 영속화 모델
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "orders")
public class OrderEntity extends BaseEntity {

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
    private List<OrderItemEntity> orderItems = new ArrayList<>();

    @Column(name = "customer_id", nullable = false, columnDefinition = "BIGINT NOT NULL COMMENT '구매자 ID'")
    private Long customerId;

    @Builder
    public OrderEntity(String orderNumber, Long totalAmount, LocalDateTime orderedAt, OrderStatus status,
                       List<OrderItemEntity> orderItems, Long customerId) {
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.orderedAt = orderedAt;
        this.status = status;
        this.orderItems = orderItems;
        this.customerId = customerId;
    }

    public static OrderEntity from(Order order) {
        var orderItems = order.orderItems().stream().map(orderItem ->
                OrderItemEntity.builder()
                        .productId(orderItem.productId())
                        .name(orderItem.name())
                        .price(orderItem.price())
                        .quantity(orderItem.quantity())
                        .build())
                .toList();

        return OrderEntity.builder()
                .orderNumber(order.orderNumber())
                .totalAmount(order.totalAmount())
                .orderedAt(order.orderedAt())
                .status(order.status())
                .orderItems(orderItems)
                .customerId(order.customerId())
                .build();
    }
}
