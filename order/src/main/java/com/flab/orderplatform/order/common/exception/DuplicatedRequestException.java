package com.flab.orderplatform.order.common.exception;

/**
 * 멱등키(idempotentKey)에 대해 동일 요청이 들어왔을 경우 던지는 예외
 */
public class DuplicatedRequestException extends BusinessException {
    public DuplicatedRequestException(String key) {
        super(String.format("이미 처리중인 요청입니다 (idempotentKey=%s)", key));
    }
}
