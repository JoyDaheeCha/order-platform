package com.flab.orderplatform.order.application.port.out;

public interface MessageProducer {
    void sendMessage(String topic, String payload);
}
