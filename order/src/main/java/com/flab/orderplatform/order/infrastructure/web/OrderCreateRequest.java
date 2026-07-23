package com.flab.orderplatform.order.infrastructure.web;

import com.flab.orderplatform.order.application.command.OrderCreateCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.util.List;

/**
 * 주문 생성
 *
 * @param customerId    구매자 PK
 * @param orderItemDtos 주문 상품 목록
 */
@Builder
public record OrderCreateRequest(
        @NotNull(message = "주문자 id는 필수입니다.")
        Long customerId,

        @Valid
        @NotNull(message = "주문상품 정보 목록은 필수입니다.")
        List<OrderItemDto> orderItemDtos
) {
    /**
     * 주문한 상품
     * @param quantity    주문 수량
     * @param name        상품명
     * @param productCode 상품 코드
     */
    @Builder
    public record OrderItemDto(
            @NotNull(message = "주문 수량은 필수입니다.")
            @Positive(message = "주문 수량은 양수이어야합니다.")
            Integer quantity,
            @NotNull(message = "상품명은 필수입니다.")
            String name,
            @NotNull(message = "상품코드는 필수입니다.")
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
