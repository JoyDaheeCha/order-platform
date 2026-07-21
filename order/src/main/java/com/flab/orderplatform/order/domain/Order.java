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
    @JoinColumn(name = "order_id")
    private List<OrderItem> orderItems = new ArrayList<>();

    @Column(name = "customer_id", nullable = false, columnDefinition = "BIGINT NOT NULL COMMENT '구매자 ID'")
    private Long customerId;

    @Builder
    public Order(String orderNumber, Long totalAmount, LocalDateTime orderedAt, OrderStatus status,
                 List<OrderItem> orderItems, Long customerId) {
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.orderedAt = orderedAt;
        this.status = status;
        this.orderItems = orderItems;
        this.customerId = customerId;
    }

    // TODO: 도메인 로직 테스트 추가 (주문번호 지정, 주문일자 현재, status pending이다 확인)
    public static Order create(Long customerId,
                               List<OrderItem> orderItems,
                               String orderNumber) {

        var totalAmount = orderItems.stream()
                .mapToLong(OrderItem::getPrice)
                .sum();
        return Order.builder()
                .customerId(customerId)
                .orderNumber(orderNumber)
                .orderItems(orderItems)
                .orderedAt(LocalDateTime.now()) // TODO 테스트 가능한 형태로 변경 (orderedAt 주입 받도록)
                .status(PENDING)
                .totalAmount(totalAmount)
                .build();
    }
}
