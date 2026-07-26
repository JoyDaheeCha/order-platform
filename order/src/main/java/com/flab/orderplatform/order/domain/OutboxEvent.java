package com.flab.orderplatform.order.domain;

import com.flab.orderplatform.order.common.JsonUtils;
import com.flab.orderplatform.order.domain.status.OutboxEventStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import static com.flab.orderplatform.order.domain.status.OutboxEventStatus.FAILED;
import static com.flab.orderplatform.order.domain.status.OutboxEventStatus.PUBLISHED;

/**
 * 아웃박스 패턴에서 도메인 이벤트 페이로드를 저장하기 위한 테이블
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicUpdate
@Entity
@Table(name = "outbox")
public class OutboxEvent extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_type", length = 30, nullable = false,
            columnDefinition = "VARCHAR(30) COMMENT '에그리거트명 (예. order)'")
    private String aggregateType;

    @Column(name = "aggregate_id", length = 30, nullable = false, columnDefinition = "VARCHAR(30) COMMENT '에그리거트 식별자'")
    private String aggregateId;

    @Column(name = "event_type", length = 30, nullable = false, columnDefinition = "VARCHAR(30) COMMENT '이벤트 타입'")
    private String eventType;

    @Column(name = "topic", length = 30, nullable = false, columnDefinition = "VARCHAR(30) COMMENT '토픽명'")
    private String topic;

    @Column(name = "payload", nullable = false, columnDefinition = "JSON NOT NULL COMMENT '이벤트 페이로드'")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false, columnDefinition = "VARCHAR(10) NOT NULL COMMENT '이벤트 상태 (CREATED/PUBLISHED/FAILED)'")
    private OutboxEventStatus status;

    @SuppressWarnings("unused")
    @Builder
    public OutboxEvent(String aggregateType, String aggregateId, String eventType, String topic, String payload, OutboxEventStatus status) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.topic = topic;
        this.payload = payload;
        this.status = status;
    }

    public static OutboxEvent create(DomainEvent domainEvent) {
        return OutboxEvent.builder()
                .aggregateType("order") // TODO: 추후 재고, 결제에서 outbound 패턴 동일 적용시 본 문자열에 대해 각각 도메인에 맞게 변경 필요
                .aggregateId(domainEvent.getAggregateId())
                .eventType(domainEvent.getAction())
                .topic(domainEvent.getTopic())
                .payload(JsonUtils.toJson(domainEvent))
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
}
