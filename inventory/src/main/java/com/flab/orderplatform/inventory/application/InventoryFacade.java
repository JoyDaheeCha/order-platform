package com.flab.orderplatform.inventory.application;

import com.flab.orderplatform.inventory.application.annotation.InventoryTransactional;
import com.flab.orderplatform.inventory.application.command.InventoryDecreaseCommand;
import com.flab.orderplatform.inventory.application.command.InventoryReserveCommand;
import com.flab.orderplatform.inventory.application.exception.DuplicatedProductException;
import com.flab.orderplatform.inventory.application.exception.InventoryNotFoundException;
import com.flab.orderplatform.inventory.application.port.out.InventoryHistoryRepository;
import com.flab.orderplatform.inventory.application.port.out.InventoryRepository;
import com.flab.orderplatform.inventory.domain.Inventory;
import com.flab.orderplatform.inventory.domain.event.StockDeductedEvent;
import com.flab.orderplatform.shared.event.OrderCreatedPayload;
import com.flab.orderplatform.shared.event.OrderPaidPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.flab.orderplatform.inventory.domain.type.InventoryUpdateRequestType.DECREASE;
import static com.flab.orderplatform.inventory.domain.type.InventoryUpdateRequestType.RESERVE;

@Component
@RequiredArgsConstructor
public class InventoryFacade {

    private final InventoryRepository inventoryRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;
    private final InventoryCommandHandler inventoryCommandHandler;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 재고 차감
     *
     * @param event 주문이 결제되었다 이벤트
     * @return 재고 목록
     */
    @InventoryTransactional
    public List<Inventory> decreaseStock(OrderPaidPayload event) {
        // 이미 재고가 차감된 주문으로 처리하지 않는다.
        if (inventoryHistoryRepository.existsByOrderNumber(event.orderNumber(), DECREASE)) {
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

    /**
     * 재고 선점
     *
     * @param event 주문이 생성되었다 이벤트
     * @return 재고 리스트
     */
    @InventoryTransactional
    public List<Inventory> reserveInventory(OrderCreatedPayload event) {
        // 이미 재고가 차감된 주문으로 처리하지 않는다.
        if (inventoryHistoryRepository.existsByOrderNumber(event.orderNumber(), RESERVE)) {
            return List.of();
        }

        // 재고 선점 요청된 모든 상품이 존재하는지 검증
        var productCodes = event.orderItems()
                .stream()
                .map(OrderCreatedPayload.OrderItem::productCode)
                .collect(Collectors.toSet());
        validateIfAllProductsExisting(productCodes);

        var commands = event.orderItems().stream().map(item -> InventoryReserveCommand.builder()
                        .orderNumber(event.orderNumber())
                        .product(
                                InventoryReserveCommand.ProductDto
                                        .builder()
                                        .productCode(item.productCode())
                                        .quantity(item.quantity())
                                        .build())
                        .build())
                .toList();

        var result = commands.stream()
                .map(inventoryCommandHandler::handle)
                .toList();

        var inventoryReservedEvent = StockDeductedEvent.builder()
                .orderNumber(event.orderNumber())
                .occurredOn(LocalDateTime.now())
                .build();
        eventPublisher.publishEvent(inventoryReservedEvent);

        return result;
    }
}
