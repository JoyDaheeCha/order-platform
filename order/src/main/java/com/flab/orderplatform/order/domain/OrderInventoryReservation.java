package com.flab.orderplatform.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Embeddable
@NoArgsConstructor(access = PROTECTED)
public class OrderInventoryReservation {

    @Column(name = "reserved_at", columnDefinition = "DATETIME(6) NOT NULL COMMENT '재고 선점 일시'")
    private LocalDateTime reservedAt;

    @Column(name = "is_released", columnDefinition = "TINYINT NOT NULL DEFAULT 0 COMMENT '재고 선점 해제 여부'")
    private boolean isReleased;

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

    public OrderInventoryReservation release() {
        this.isReleased = true;
        return this;
    }
}
