package com.flab.orderplatform.order.application.command;

import com.flab.orderplatform.order.domain.Order;
import lombok.Builder;

import java.util.Map;

@Builder
public record OrderPayCommand(
        String orderNumber,
        Map<Long, String> productMapCodeById
) {
    public Order pay(Order order) {
        return order.pay(productMapCodeById);
    }
}
