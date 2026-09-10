package com.flab.orderplatform.order.domain;

import com.flab.orderplatform.order.domain.status.OrderInventoryReservationReleaseReason;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Embeddable
@NoArgsConstructor(access = PROTECTED)
public class OrderInventoryReservation {

    @Column(name = "reserved_at", columnDefinition = "DATETIME(6) NOT NULL COMMENT '재고 선점 일시'")
    private LocalDateTime reservedAt;

    @Column(name = "is_released", columnDefinition = "TINYINT(1) COMMENT '재고 선점 해제 여부'")
    private Boolean isReleased;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_release_reason", length = 30, columnDefinition = "VARCHAR(30) COMMENT '재고 선점 해제 사유'")
    private OrderInventoryReservationReleaseReason releaseReason;

    @Builder
    public OrderInventoryReservation(LocalDateTime reservedAt, boolean isReleased) {
        this.reservedAt = reservedAt;
        this.isReleased = isReleased;
    }

    public static OrderInventoryReservation create(LocalDateTime reservedAt) {
        return OrderInventoryReservation.builder()
                .reservedAt(reservedAt)
                .isReleased(false)
                .build();
    }

    public OrderInventoryReservation release(OrderInventoryReservationReleaseReason reason) {
        this.isReleased = true;
        this.releaseReason = reason;
        return this;
    }
}
