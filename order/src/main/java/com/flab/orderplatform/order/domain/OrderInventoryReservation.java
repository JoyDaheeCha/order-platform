package com.flab.orderplatform.order.domain;

import com.flab.orderplatform.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Order 컨텍스트의 영속화 모델
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "order_inventory_reservation")
public class OrderInventoryReservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", length = 36, nullable = false, unique = true,
            columnDefinition = "VARCHAR(36)  NOT NULL COMMENT '주문번호 (대외 노출용 비즈니스 키)'")
    private String orderNumber;

    @Column(name = "reserved_at", nullable = false, columnDefinition = "DATETIME(6) NOT NULL COMMENT '재고 선점 일시'")
    private LocalDateTime reservedAt;

    @Column(name = "is_released", nullable = false, columnDefinition = "TINYINT NOT NULL COMMENT '재고 선점 해제 여부'")
    private boolean isReleased;

    @Builder
    public OrderInventoryReservation(String orderNumber, LocalDateTime reservedAt, boolean isReleased) {
        this.orderNumber = orderNumber;
        this.reservedAt = reservedAt;
        this.isReleased = isReleased;
    }

    public static OrderInventoryReservation create(String orderNumber, LocalDateTime reservedAt) {
        return OrderInventoryReservation.builder()
                .orderNumber(orderNumber)
                .reservedAt(reservedAt)
                .isReleased(false)
                .build();
    }
}
