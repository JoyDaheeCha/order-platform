package com.flab.orderplatform.order.application.aspect;

import java.io.Serializable;

/**
 * 멱등성 검사 aspect {@link IdempotentAspect} 에서 key=멱등키, value=메서드 진행 상태 로 저장할때 value 에 들어갈 래퍼 클래스
 * @param status
 * @param payload
 */
public record IdempotentEntry(Status status, Object payload) implements Serializable {
    public static IdempotentEntry inProgress() {
        return new IdempotentEntry(Status.IN_PROGRESS, null);
    }

    public static IdempotentEntry completed(Object payload) {
        return new IdempotentEntry(Status.COMPLETED, payload);
    }

    public boolean isCompleted() {
        return status == Status.COMPLETED;
    }

    public enum Status {
        IN_PROGRESS,
        COMPLETED
    }
}
