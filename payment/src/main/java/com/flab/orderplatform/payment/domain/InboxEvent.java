package com.flab.orderplatform.payment.domain;

import com.flab.orderplatform.payment.domain.status.InboxEventStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.flab.orderplatform.payment.domain.status.InboxEventStatus.*;
import static jakarta.persistence.GenerationType.IDENTITY;

/**
 * 인박스 패턴 구현용 테이블
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "inbox")
public class InboxEvent extends BaseTimeEntity {
    @GeneratedValue(strategy = IDENTITY)
    @Id
    Long id;

    @Column(name = "event_id", length = 36, nullable = false, unique = true, columnDefinition = "CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '이벤트 UUID'")
    String eventId;

    @Column(name = "aggregate_type", length = 30, nullable = false, columnDefinition = "VARCHAR(30) NOT NULL COMMENT '에그리거트명 (예. payment)'")
    String aggregateType;

    @Column(name = "event_type", length = 30, nullable = false, columnDefinition = "VARCHAR(30) NOT NULL COMMENT '이벤트타입(예.OrderCreated)'")
    String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "LONGTEXT NOT NULL COMMENT '이벤트 페이로드'")
    String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(10) NOT NULL COMMENT '이벤트 상태 (CREATED/PROCESSED/FAILED)'")
    InboxEventStatus status;

    @Builder
    public InboxEvent(String eventId,
                      String aggregateType,
                      String eventType,
                      String payload,
                      InboxEventStatus status) {
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.payload = payload;
        this.status = status;
    }

    public static InboxEvent create(String eventId,
                                    String aggregateType,
                                    String eventType,
                                    String payload) {

        return InboxEvent
                .builder()
                .eventId(eventId)
                .aggregateType(aggregateType)
                .eventType(eventType)
                .payload(payload)
                .status(CREATED)
                .build();
    }

    public InboxEvent succeed() {
        this.status = PROCESSED;
        return this;
    }
    public InboxEvent fail() {
        this.status = FAILED;
        return this;
    }
}
