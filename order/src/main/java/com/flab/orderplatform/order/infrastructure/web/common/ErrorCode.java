package com.flab.orderplatform.order.infrastructure.web.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    NOT_FOUND(HttpStatus.BAD_REQUEST, "요청한 값이 존재하지 않습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "사용자 입력값이 올바르지 않습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
