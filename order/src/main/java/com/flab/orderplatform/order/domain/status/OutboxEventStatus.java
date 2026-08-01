package com.flab.orderplatform.order.domain.status;

/**
 * 아웃박스 패턴에서 사용되는 이벤트 발행 상태
 */
public enum OutboxEventStatus {
    CREATED("생성완료"),
    PUBLISHED("발행완료"),
    FAILED("발행실패");

    private final String description;

    OutboxEventStatus(String description) {
        this.description = description;
    }
}
