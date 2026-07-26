package com.flab.orderplatform.order.application.port.out;

public interface MessageProducer {
    void sendMessage(String topic, Object payload);
}
