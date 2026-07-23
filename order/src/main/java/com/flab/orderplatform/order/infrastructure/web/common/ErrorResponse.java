package com.flab.orderplatform.order.infrastructure.web.common;

import lombok.Builder;

import java.util.List;

@Builder
public record ErrorResponse(
        String code,
        String message,
        List<Violation> violations
) {
    @Builder
    public record Violation(
            String field,
            String value,
            String reason
    ) {
    }

    public static ErrorResponse of(ErrorCode errorCode, List<Violation> violations) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), violations);
    }
}
