package com.flab.orderplatform.order.domain;

import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 도메인에서 이벤트가 발생했을 때 메시지를 발행하기 위한 메타클래스
 */
@Getter
public abstract class DomainEvent implements Serializable {
    private final String eventId;
    private final String aggregateId;
    private final LocalDateTime occurredOn;

    protected DomainEvent(String aggregateId, LocalDateTime occurredOn) {
        this.eventId = UUID.randomUUID().toString();
        this.aggregateId = aggregateId;
        this.occurredOn = occurredOn;
    }

    public abstract String getAction();

    public abstract String getTopic();
}
