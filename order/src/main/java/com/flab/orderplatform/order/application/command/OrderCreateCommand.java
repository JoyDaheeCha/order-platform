package com.flab.orderplatform.order.application.command;

import com.flab.orderplatform.order.domain.Order;
import com.flab.orderplatform.order.domain.OrderItem;
import com.flab.orderplatform.order.domain.external.Product;
import lombok.Builder;

import java.time.Clock;
import java.util.List;
import java.util.Map;

/**
 * 주문 생성
 *
 * @param customerId 구매자 PK
 * @param orderItems 주문 상품 목록
 */
@Builder
public record OrderCreateCommand(
        Long customerId,
        List<OrderItemDto> orderItems
) {
    /**
     * 주문한 상품
     *
     * @param quantity    주문 수량
     * @param name        상품명
     * @param productCode 상품 코드
     */
    @Builder
    public record OrderItemDto(
            Integer quantity,
            String name,
            String productCode
    ) {
    }

    public Order createOrder(String orderNumber, Map<String, Product> productsMap, Clock clock) {
        var orderItems = this.orderItems.stream().map(item -> {
                    var product = productsMap.get(item.productCode);
                    return OrderItem.builder()
                            .quantity(item.quantity)
                            .name(item.name)
                            .productId(product.getId())
                            .price(product.getPrice())
                            .quantity(item.quantity)
                            .build();
                })
                .toList();
        return Order.create(customerId, orderItems, orderNumber, clock);
    }

    public List<String> getProductCodes() {
        return this.orderItems.stream()
                .map(OrderItemDto::productCode)
                .toList();
    }
}
