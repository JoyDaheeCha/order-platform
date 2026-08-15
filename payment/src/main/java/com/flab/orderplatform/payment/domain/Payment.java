package com.flab.orderplatform.payment.domain;

import com.flab.orderplatform.payment.domain.event.PaymentCompletedEvent;
import com.flab.orderplatform.payment.domain.status.PaymentStatus;
import com.flab.orderplatform.shared.domain.BaseTimeEntity;
import com.flab.orderplatform.shared.domain.DomainEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.flab.orderplatform.payment.domain.status.PaymentStatus.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "payment",
        indexes = {
                @Index(name = "idx_payment_status", columnList = "status")
        })
public class Payment extends BaseTimeEntity {

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
    @Column(name = "status", nullable = false, length = 20, columnDefinition = "VARCHAR(10) NOT NULL COMMENT '결제 상태 REQUESTED/COMPLETED/FAILED/REFUNDED'")
    private PaymentStatus status;

    @Column(name = "failure_reason", length = 50, columnDefinition = "VARCHAR(50) COMMENT '결제 실패 사유'")
    private String failureReason;

    @Column(name = "pg_tid", columnDefinition = "VARCHAR(36) COMMENT 'PG사 결제 ID(tid)'")
    private String pgTid;

    @Column(name = "pg_requested_at", columnDefinition = "DATETIME(6) COMMENT 'pg 결제 요청 시각'")
    private LocalDateTime pgRequestedAt;

    @Transient
    private DomainEvent domainEvent;

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
                .failureReason(null)
                .build();
    }

    public Payment complete(Boolean isPaymentSucceed, String failureReason, String pgTid) {
        if (this.status != IN_PROGRESS) {
            throw new IllegalStateException("결제 처리중일때만 완료 처리 가능합니다. (현재 상태: %s)"
                    .formatted(status.getDescription()));
        }
        this.pgTid = pgTid;
        if (isPaymentSucceed) {
            this.status = COMPLETED;
            this.failureReason = null;
            registerPaymentCompletedEvent();
            return this;
        }
        this.status = FAILED;
        this.failureReason = failureReason;
        return this;
    }

    public boolean isCompleted() {
        return this.status == COMPLETED;
    }

    public boolean isFailed() {
        return this.status == FAILED;
    }

    private void registerPaymentCompletedEvent() {
        this.domainEvent = PaymentCompletedEvent.builder()
                .orderNumber(orderNumber)
                .pgTid(pgTid)
                .amount(amount)
                .aggregateId(String.valueOf(id))
                .occurredOn(LocalDateTime.now())
                .build();
    }

    public Payment retry() {
        if (this.status != FAILED) {
            throw new IllegalStateException("실패한 결제만 재시도 가능합니다. (현재상태: %s)".formatted(status.getDescription()));
        }
        this.status = REQUESTED;
        this.failureReason = null;
        return this;
    }

    public Optional<DomainEvent> pullDomainEventIfPresent() {
        var event = Optional.ofNullable(domainEvent);
        domainEvent = null;
        return event;
    }

    public Payment start() {
        if (status != REQUESTED) {
            throw new IllegalStateException("'결제대기' 상태만 '결제 처리중'으로 변경 가능합니다. (현재 상태 : %s)".formatted(status.getDescription()));
        }
        status = IN_PROGRESS;
        pgRequestedAt = LocalDateTime.now();
        return this;
    }
}
