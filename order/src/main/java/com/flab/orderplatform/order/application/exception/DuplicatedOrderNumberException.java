package com.flab.orderplatform.order.application.exception;


/**
 * 주문번호 생성시, 중복된 번호가 생성될 경우 서버 예외로 처리
 */
public class DuplicatedOrderNumberException extends RuntimeException {

    public DuplicatedOrderNumberException(Throwable cause) {
        super("주문 생성에 실패했습니다. 잠시 후 다시 시도해주세요", cause);
    }
}
