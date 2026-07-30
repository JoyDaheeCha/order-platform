package com.flab.orderplatform.payment.application.exception;

import com.flab.orderplatform.payment.common.BusinessErrorCode;
import com.flab.orderplatform.payment.common.BusinessException;

import static com.flab.orderplatform.payment.common.BusinessErrorCode.NOT_FOUND;

public class InboxEventNotFoundException extends BusinessException {

    public InboxEventNotFoundException(String eventId) {
        super(String.format("이벤트 id(%s)에 대해 존재하는 이벤트가 없습니다.", eventId));
    }

    @Override
    public BusinessErrorCode getErrorCode() {
        return NOT_FOUND;
    }
}
