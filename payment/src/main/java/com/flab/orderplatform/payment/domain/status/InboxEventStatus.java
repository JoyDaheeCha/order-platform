package com.flab.orderplatform.payment.domain.status;

import lombok.Getter;

/**
 * 인박스 내 데이터 처리 상태
 */
@Getter
public enum InboxEventStatus {
    CREATED("생성완료"),
    PROCESSED("비즈니스 로직 처리 완료"),
    FAILED("비즈니스 로직 실행 실패");

    private final String description;

    InboxEventStatus(String description) {
        this.description = description;
    }
}
