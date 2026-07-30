package com.flab.orderplatform.payment.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum BusinessErrorCode {
    NOT_FOUND(HttpStatus.BAD_REQUEST, "요청한 값이 존재하지 않습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "사용자 입력값이 올바르지 않습니다."),
    CONFLICT_REQUEST(HttpStatus.CONFLICT, "이미 처리중인 요청입니다.");

    private final HttpStatus status;
    private final String message;

    BusinessErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
