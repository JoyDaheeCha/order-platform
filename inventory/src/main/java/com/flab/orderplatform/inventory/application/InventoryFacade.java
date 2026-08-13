package com.flab.orderplatform.inventory.application;

import com.flab.orderplatform.inventory.application.annotation.InventoryTransactional;
import com.flab.orderplatform.inventory.application.command.InventoryDecreaseCommand;
import com.flab.orderplatform.inventory.application.exception.DuplicatedProductException;
import com.flab.orderplatform.inventory.application.port.out.InventoryHistoryRepository;
import com.flab.orderplatform.inventory.domain.Inventory;
import com.flab.orderplatform.inventory.domain.event.StockDeductedEvent;
import com.flab.orderplatform.shared.event.OrderPaidPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InventoryFacade {

    private final InventoryHistoryRepository inventoryHistoryRepository;
    private final InventoryDecreaseLockResolver lockResolver;
    private final ApplicationEventPublisher eventPublisher;

    @InventoryTransactional
    public List<Inventory> decreaseStock(OrderPaidPayload event) {
        // 이미 재고가 차감된 주문으로 처리하지 않는다.
        if (inventoryHistoryRepository.existsByOrderNumber(event.orderNumber())) {
            return List.of();
        }
        // 재고 감소시 상품 정보를 중복하여 넣을 수 없다.
        validateDuplicatedProductCode(event);

        var orderNumber = event.orderNumber();
        var commands = getCommands(event, orderNumber);

        var result = lockResolver.resolve().handle(commands);

        var stockDeductedEvent = StockDeductedEvent.builder()
                .orderNumber(orderNumber)
                .occurredOn(LocalDateTime.now())
                .build();
        eventPublisher.publishEvent(stockDeductedEvent);
        return result;
    }

    private void validateDuplicatedProductCode(OrderPaidPayload event) {
        var productCodes = event.orderItems()
                .stream()
                .map(OrderPaidPayload.OrderItemDto::productCode)
                .collect(Collectors.toSet()); // 주문 내에서 상품 번호는 유니크하므로 set으로 설정

        if (event.orderItems().size() != productCodes.size()) {
            throw new DuplicatedProductException(productCodes);
        }
    }

    private List<InventoryDecreaseCommand> getCommands(OrderPaidPayload event, String orderNumber) {
        return event.orderItems().stream().map(item -> InventoryDecreaseCommand.builder()
                        .orderNumber(orderNumber)
                        .product(InventoryDecreaseCommand.ProductDto
                                .builder()
                                .productCode(item.productCode())
                                .quantityToDecrease(item.quantity())
                                .build())
                        .build())
                .toList();
    }
}
