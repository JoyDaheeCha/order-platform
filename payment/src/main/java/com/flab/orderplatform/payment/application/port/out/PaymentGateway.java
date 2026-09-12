package com.flab.orderplatform.payment.application.port.out;

/**
 * 결제 시스템
 *
 * 본 프로젝트에서는 PG를 직접 연동하지 않고, mock 으로 대체합니다.
 */
public interface PaymentGateway {
    PgApprovalResult approve(PgApprovalRequest request);
    PgSearchResult get(String orderNumber);
    PgRefundResult refund(PgRefundRequest request);
}
