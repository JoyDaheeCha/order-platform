package com.flab.orderplatform.order.application.port.out.view;

import com.flab.orderplatform.order.domain.status.OrderStatus;
import com.querydsl.core.annotations.QueryProjection;

import java.util.List;

public record OrderView(
        String orderNumber,
        OrderStatus status,
        List<OrderItemDto> orderItems
) {
    public record OrderItemDto(
            String productName,
            Integer quantity,
            Long price
    ) {
        @QueryProjection
        public OrderItemDto {
        }
    }
}
