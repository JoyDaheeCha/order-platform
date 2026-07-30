package com.flab.orderplatform.order.domain;

import com.flab.orderplatform.order.common.JsonUtils;
import com.flab.orderplatform.order.domain.status.OutboxEventStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.flab.orderplatform.order.domain.status.OutboxEventStatus.FAILED;
import static com.flab.orderplatform.order.domain.status.OutboxEventStatus.PUBLISHED;
import static com.flab.orderplatform.shared.event.EventConstants.AGGREGATE_ORDER;
import static com.flab.orderplatform.shared.event.EventConstants.Headers.*;

/**
 * 아웃박스 패턴에서 도메인 이벤트 페이로드를 저장하기 위한 테이블
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicUpdate
@Entity
@Table(
        name = "outbox",
        indexes = {
                @Index(name ="idx_outbox_1", columnList = "status, created_at")
        }
)
public class OutboxEvent extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", length = 36, nullable = false, unique = true,
            columnDefinition = "CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '이벤트 UUID'")
    private String eventId;

    @Column(name = "aggregate_type", length = 30, nullable = false,
            columnDefinition = "VARCHAR(30) COMMENT '에그리거트명 (예. order)'")
    private String aggregateType;

    @Column(name = "aggregate_id", length = 36, nullable = false, columnDefinition = "VARCHAR(36) COMMENT '에그리거트 식별자'")
    private String aggregateId;

    @Column(name = "event_type", length = 30, nullable = false, columnDefinition = "VARCHAR(30) COMMENT '이벤트 타입'")
    private String eventType;

    @Column(name = "topic", length = 50, nullable = false, columnDefinition = "VARCHAR(50) COMMENT '토픽명'")
    private String topic;

    @Column(name = "payload", nullable = false, columnDefinition = "JSON NOT NULL COMMENT '이벤트 페이로드'")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false, columnDefinition = "VARCHAR(10) NOT NULL COMMENT '이벤트 상태 (CREATED/PUBLISHED/FAILED)'")
    private OutboxEventStatus status;

    @Column(name = "occurred_at", nullable = false, columnDefinition = "DATETIME(6) NOT NULL COMMENT '이벤트 발생일시'")
    private LocalDateTime occurredAt;

    @SuppressWarnings("unused")
    @Builder
    public OutboxEvent(String eventId, String aggregateType, String aggregateId, String eventType, String topic,
                       String payload, OutboxEventStatus status, LocalDateTime occurredAt) {
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.topic = topic;
        this.payload = payload;
        this.status = status;
        this.occurredAt = occurredAt;
    }

    public static OutboxEvent create(DomainEvent domainEvent) {
        var payload = domainEvent.toPayload();
        return OutboxEvent.builder()
                .eventId(domainEvent.getEventId())
                .aggregateType(AGGREGATE_ORDER)
                .aggregateId(domainEvent.getAggregateId())
                .eventType(payload.eventType())
                .topic(payload.topic())
                .payload(JsonUtils.toJson(payload))
                .status(OutboxEventStatus.CREATED)
                .occurredAt(domainEvent.getOccurredOn())
                .build();
    }

    /**
     * 메시지 발행완료 처리
     */
    public OutboxEvent complete() {
        this.status = PUBLISHED;
        return this;
    }

    /**
     * 메시지 발행실패 처리
     */
    public OutboxEvent fail() {
        this.status = FAILED;
        return this;
    }

    public HashMap<String, String> toMessageHeaders() {
        return new HashMap<>(Map.of(
                EVENT_ID, eventId,
                AGGREGATE_TYPE, aggregateType,
                EVENT_TYPE, eventType,
                OCCURRED_AT, occurredAt.toString()));
    }
}
