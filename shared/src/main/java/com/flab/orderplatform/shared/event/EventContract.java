package com.flab.orderplatform.shared.event;

/**
 * 도메인간 이벤트 통신 규약
 */
public interface EventContract {
    String eventType();
    String topic();
}
