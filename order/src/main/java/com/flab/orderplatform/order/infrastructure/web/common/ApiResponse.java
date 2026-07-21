package com.flab.orderplatform.order.infrastructure.web.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Api 응답 최상위. (봉투패턴)
 * 성공이면 erorr가, 실패면 data가 직렬화에서 빠진다
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorResponse error
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> fail(ErrorResponse error) {
        return new ApiResponse<>(false, null, error);
    }
}
