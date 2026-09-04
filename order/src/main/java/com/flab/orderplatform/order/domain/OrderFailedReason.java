package com.flab.orderplatform.order.domain;

import com.flab.orderplatform.order.domain.status.OrderFailedReasonType;
import com.flab.orderplatform.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 실패 사유
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "order_failed_reason")
public class OrderFailedReason extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false, columnDefinition = "VARCHAR(50)  NOT NULL COMMENT '주문 실패 사유 (INVENTORY_SHORTAGE/PAYMENT_FAILED)'")
    private OrderFailedReasonType reason;

    public OrderFailedReason(OrderFailedReasonType reason) {
        this.reason = reason;
    }

    public static OrderFailedReason create(OrderFailedReasonType reason) {
        return new OrderFailedReason(reason);
    }
}
