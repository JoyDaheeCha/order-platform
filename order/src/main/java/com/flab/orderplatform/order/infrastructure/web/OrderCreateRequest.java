package com.flab.orderplatform.order.infrastructure.web;

import com.flab.orderplatform.order.application.command.OrderCreateCommand;
import com.flab.orderplatform.order.domain.OrderItem;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 주문 생성
 *
 * @param customerId    구매자 PK
 * @param orderItemDtos 주문 상품 목록
 */
public record OrderCreateRequest(
        @NotNull
        Long customerId,
        @NotNull
        List<OrderItemDto> orderItemDtos
) {
    /**
     * 주문한 상품
     * @param quantity    주문 수량
     * @param name        상품명
     * @param productCode 상품 코드
     */
    public record OrderItemDto(
            @NotNull
            Integer quantity,
            @NotNull
            String name,
            @NotNull
            String productCode
    ) {
    }

    public OrderCreateCommand toCommand() {
        return OrderCreateCommand.builder()
                .customerId(this.customerId)
                .orderItems(this.orderItemDtos.stream().map(item ->
                                OrderCreateCommand.OrderItemDto.builder()
                                        .quantity(item.quantity)
                                        .name(item.name)
                                        .productCode(item.productCode).build())
                        .toList()
                )
                .build();
    }
}
