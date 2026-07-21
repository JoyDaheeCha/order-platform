package com.flab.orderplatform.order.infrastructure.persistence;

import java.time.LocalDateTime;

import com.flab.orderplatform.order.domain.Order;
import com.flab.orderplatform.order.domain.status.OrderStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Order 컨텍스트의 영속화 모델
 * TODO: Day3 작업에서 order 로직 넣을때, 필요한 컬럼 추가. (현재는 order, payment, inventory간 경계가 물리적으로 제약되어 있는지 테스트할 용도로 일부 필드만 넣어두었음.)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "orders")
public class OrderEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", length = 36, nullable = false, unique = true, // TODO: 주문 번호 varchar(19)로 조정 필요할지 검토
            columnDefinition = "VARCHAR(36)  NOT NULL COMMENT '주문번호 (대외 노출용 비즈니스 키)'")
    private String orderNumber;

    @Column(name = "total_amount", nullable = false, columnDefinition = "BIGINT NOT NULL COMMENT '총 구매 금액'")
    private long totalAmount;

    @Column(name = "ordered_at", nullable = false, columnDefinition = "DATETIME(6) NOT NULL COMMENT '주문일자'")
    private LocalDateTime orderedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false, columnDefinition = "VARCHAR(20)  NOT NULL COMMENT '주문 상태 (PENDING/PAID/CONFIRMED/CANCELLED)'")
    private OrderStatus status;

    @Builder
    public OrderEntity(String orderNumber, long totalAmount, LocalDateTime orderedAt, OrderStatus status) {
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.orderedAt = orderedAt;
        this.status = status;
    }

    public static OrderEntity from(Order order) {
        return OrderEntity.builder()
                .orderNumber(order.orderNumber())
                .totalAmount(0)// TODO 상품 테이블 에서 채워넣기.
                .orderedAt(order.orderedAt())
                .status(order.status())
                .build();
    }
}
