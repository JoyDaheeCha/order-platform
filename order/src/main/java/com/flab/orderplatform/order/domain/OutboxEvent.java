package com.flab.orderplatform.order.domain;

import com.flab.orderplatform.order.common.JsonUtils;
import com.flab.orderplatform.order.domain.status.OutboxEventStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.util.HashMap;
import java.util.Map;

import static com.flab.orderplatform.order.domain.status.OutboxEventStatus.FAILED;
import static com.flab.orderplatform.order.domain.status.OutboxEventStatus.PUBLISHED;
import static com.flab.orderplatform.shared.event.EventConstants.AGGREGATE_ORDER;

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
    private static final String HEADER_EVENT_ID = "eventId";
    public static final String HEADER_AGGREGATE_TYPE_VALUE = "order";
    private static final String HEADER_EVENT_TYPE = "eventType";
    private static final String HEADER_AGGREGATE_TYPE = "aggregateType";

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

    @SuppressWarnings("unused")
    @Builder
    public OutboxEvent(String eventId, String aggregateType, String aggregateId, String eventType, String topic, String payload, OutboxEventStatus status) {
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.topic = topic;
        this.payload = payload;
        this.status = status;
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

    public Map<String, String> toMessageHeaders() {
        var map = new HashMap<>(Map.of(HEADER_EVENT_ID, eventId));
        map.put(HEADER_AGGREGATE_TYPE, aggregateType);
        map.put(HEADER_EVENT_TYPE, eventType);
        return map;
    }
}
