package com.flab.orderplatform.payment.domain;

import com.flab.orderplatform.payment.domain.status.PaymentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.flab.orderplatform.payment.domain.status.PaymentStatus.REQUESTED;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "payments")
public class Payment extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", length = 36, nullable = false, unique = true,
            columnDefinition = "VARCHAR(36)  NOT NULL COMMENT '주문번호 (대외 노출용 비즈니스 키)'")
    private String orderNumber;

    @Column(name = "buyer_id", nullable = false, columnDefinition = "BIGINT NOT NULL COMMENT '구매자 id'")
    private Long buyerId;

    @Column(name = "amount", nullable = false, columnDefinition = "BIGINT NOT NULL COMMENT '총 결제액'")
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10, columnDefinition = "VARCHAR(10) NOT NULL COMMENT '결제 상태 REQUESTED/COMPLETED/FAILED/REFUNDED'")
    private PaymentStatus status;

    @Column(name = "failure_reason", length = 50, columnDefinition = "VARCHAR(50) COMMENT '결제 실패 사유'")
    private String failureReason;

    @Builder
    public Payment(String orderNumber, Long buyerId, Long amount, PaymentStatus status, String failureReason) {
        this.orderNumber = orderNumber;
        this.buyerId = buyerId;
        this.amount = amount;
        this.status = status;
        this.failureReason = failureReason;
    }

    public static Payment create(String orderNumber,
                          Long buyerId,
                          Long amount) {
        return Payment
                .builder()
                .orderNumber(orderNumber)
                .buyerId(buyerId)
                .amount(amount)
                .status(REQUESTED)
                .failureReason("")
                .build();
    }
}
