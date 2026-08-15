package com.flab.orderplatform.payment.application.port.out;

public class PaymentException extends RuntimeException{
    public PaymentException(String orderNumber, Throwable cause) {
        super("주문번호 %s 에 대해 PG사 연동이 실패했습니다".formatted(orderNumber), cause);
    }
}
