package com.flab.orderplatform.inventory.application;

import com.flab.orderplatform.inventory.application.annotation.InventoryTransactional;
import com.flab.orderplatform.inventory.application.command.InventoryDecreaseCommand;
import com.flab.orderplatform.inventory.application.exception.DuplicatedProductException;
import com.flab.orderplatform.inventory.application.exception.InventoryNotFoundException;
import com.flab.orderplatform.inventory.application.port.out.InventoryHistoryRepository;
import com.flab.orderplatform.inventory.application.port.out.InventoryRepository;
import com.flab.orderplatform.inventory.domain.Inventory;
import com.flab.orderplatform.inventory.domain.event.StockDeductedEvent;
import com.flab.orderplatform.shared.event.OrderPaidPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InventoryFacade {

    private final InventoryRepository inventoryRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;
    private final InventoryCommandHandler inventoryCommandHandler;
    private final ApplicationEventPublisher eventPublisher;

    @InventoryTransactional
    public List<Inventory> decreaseStock(OrderPaidPayload event) {
        // 이미 재고가 차감된 주문으로 처리하지 않는다.
        if (inventoryHistoryRepository.existsByOrderNumber(event.orderNumber())) {
            return List.of();
        }
        // 재고 감소 요청된 모든 상품이 존재하는지 유효성 검증
        var productCodes = event.orderItems()
                .stream()
                .map(OrderPaidPayload.OrderItemDto::productCode)
                .collect(Collectors.toSet()); // 주문 내에서 상품 번호는 유니크하므로 set으로 설정

        // 재고 조정시 상품 정보를 중복하여 넣을 수 없다.
        if (event.orderItems().size() != productCodes.size()) {
            throw new DuplicatedProductException(productCodes);
        }
        validateIfAllProductsExisting(productCodes);

        var orderNumber = event.orderNumber();
        var commands = event.orderItems().stream().map(item -> InventoryDecreaseCommand.builder()
                        .orderNumber(orderNumber)
                        .product(InventoryDecreaseCommand.ProductDto
                                .builder()
                                .productCode(item.productCode())
                                .quantityToDecrease(item.quantity())
                                .build())
                        .build())
                .sorted(Comparator.comparing(command -> command.product().productCode())) // 상품 코드별 정렬. 비관락 사용시 정렬 필요
                .toList();

        var result = commands.stream()
                .map(inventoryCommandHandler::handle)
                .toList();

        var stockDeductedEvent = StockDeductedEvent.builder()
                .orderNumber(orderNumber)
                .occurredOn(LocalDateTime.now())
                .build();
        eventPublisher.publishEvent(stockDeductedEvent);
        return result;
    }

    /**
     * 요청된 모든 상품이 재고로 등록되어있는지 유효성 검사
     */
    private void validateIfAllProductsExisting(Set<String> productCodes) {
        var inventories = inventoryRepository.findByProductCodeIn(productCodes);

        var inventoryNames = inventories.stream()
                .map(Inventory::getProductCode)
                .collect(Collectors.toSet());

        if (!inventoryNames.containsAll(productCodes)) {
            var missing = productCodes
                    .stream()
                    .filter(productCode -> !inventoryNames.contains(productCode))
                    .toList();
            throw new InventoryNotFoundException(missing);
        }
    }


}
