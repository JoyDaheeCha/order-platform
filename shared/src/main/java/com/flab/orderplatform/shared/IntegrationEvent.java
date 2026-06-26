package com.flab.orderplatform.shared;

import java.time.Instant;

/**
 * 컨텍스트 간 Kafka 로 오가는 모든 "통합 이벤트"의 공통 계약. (architecture.md §4, policy PI-4)
 *
 * <p>도메인 이벤트(각 컨텍스트 내부)와는 별개의 개념이다. infrastructure 가 도메인 이벤트를
 * 이 통합 이벤트로 변환해 Outbox 로 발행한다(C-4).
 *
 * <p>코레오그래피 Saga 에서 흐름이 토픽에 흩어지므로(ADR-0001 §5), {@link #orderId()} 를
 * 전 이벤트의 <b>상관관계 키</b>로 강제해 추적 가능성을 확보한다.
 */
public interface IntegrationEvent {

    /** 멱등 처리(Inbox 중복제거)용 고유 식별자. (policy PI-5) */
    String eventId();

    /** 발생 시각. */
    Instant occurredAt();

    /** Saga 상관관계 키 — 모든 통합 이벤트가 공유한다. (policy PI-4) */
    String orderId();
}
