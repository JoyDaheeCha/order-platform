package com.flab.orderplatform.inventory.application;

import com.flab.orderplatform.inventory.application.annotation.InventoryTransactional;
import com.flab.orderplatform.inventory.application.command.InventoryDecreaseCommand;
import com.flab.orderplatform.inventory.application.command.InventoryReserveCommand;
import com.flab.orderplatform.inventory.application.command.InventoryReservedRestoreCommand;
import com.flab.orderplatform.inventory.application.exception.DuplicatedProductException;
import com.flab.orderplatform.inventory.application.exception.InventoryNotFoundException;
import com.flab.orderplatform.inventory.application.port.out.InventoryHistoryRepository;
import com.flab.orderplatform.inventory.application.port.out.InventoryRepository;
import com.flab.orderplatform.inventory.domain.Inventory;
import com.flab.orderplatform.inventory.domain.event.InventoryReservationFailedEvent;
import com.flab.orderplatform.inventory.domain.event.InventoryReservedEvent;
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

import static com.flab.orderplatform.inventory.domain.type.InventoryUpdateRequestType.*;

@Component
@RequiredArgsConstructor
public class InventoryFacade {

    private final InventoryRepository inventoryRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;
    private final InventoryCommandHandler inventoryCommandHandler;
    private final ApplicationEventPublisher eventPublisher;

    // TODO: stock으로 프로젝트 내에 네이밍 된 부분은 별도 PR에서 inventory 로 리네이밍
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

        // 상품 유효성 검증
        var productCodes = event.orderItems()
                .stream()
                .map(OrderPaidPayload.OrderItemDto::productCode)
                .collect(Collectors.toSet());
        validateProductCodes(productCodes, event.orderItems().size());

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
     * 재고 조정 요청된 상품의 유효성 검증
     * @param productCodes 상품 코드 목록
     * @param orderItemsSize 상품 총 개수
     */
    private void validateProductCodes(Set<String> productCodes, int orderItemsSize) {// 재고 조정시 상품 정보를 중복하여 넣을 수 없다.
        if (orderItemsSize != productCodes.size()) {
            throw new DuplicatedProductException(productCodes);
        }
        // 재고 감소 요청된 모든 상품이 존재하는지 유효성 검증
        validateIfAllProductsExisting(productCodes);
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
        // 이미 재고가 선점된 주문으로 처리하지 않는다.
        if (inventoryHistoryRepository.existsByOrderNumber(event.orderNumber(), RESERVE)) {
            return List.of();
        }

        // 상품 유효성 검증
        var productCodes = event.orderItems()
                .stream()
                .map(OrderCreatedPayload.OrderItem::productCode)
                .collect(Collectors.toSet());
        validateProductCodes(productCodes, event.orderItems().size());

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

        var now = LocalDateTime.now();
        var inventoryReservedEvent = InventoryReservedEvent.builder()
                .orderNumber(event.orderNumber())
                .occurredOn(now)
                .reservedAt(now)
                .build();
        eventPublisher.publishEvent(inventoryReservedEvent);
        return result;
    }

    @InventoryTransactional
    public void publishReservationFailed(String orderNumber) {
        var inventoryReservationFailedEvent = InventoryReservationFailedEvent.builder()
                .orderNumber(orderNumber)
                .occurredOn(LocalDateTime.now())
                .build();
        eventPublisher.publishEvent(inventoryReservationFailedEvent);
    }

    /**
     * 선점되었던 재고를 원복한다.
     */
    @InventoryTransactional
    public List<Inventory> restoreReservedInventory(OrderPaidPayload event) {
        // 이미 재고가 선점된 주문으로 처리하지 않는다.
        if (inventoryHistoryRepository.existsByOrderNumber(event.orderNumber(), RESTORE_RESERVATION)) {
            return List.of();
        }

        // 상품 유효성 검증
        var productCodes = event.orderItems()
                .stream()
                .map(OrderPaidPayload.OrderItemDto::productCode)
                .collect(Collectors.toSet());
        validateProductCodes(productCodes, event.orderItems().size());

        var commands = event.orderItems().stream().map(item -> InventoryReservedRestoreCommand.builder()
                        .orderNumber(event.orderNumber())
                        .product(
                                InventoryReservedRestoreCommand.ProductDto
                                        .builder()
                                        .productCode(item.productCode())
                                        .quantity(item.quantity())
                                        .build())
                        .build())
                .toList();

        return commands.stream()
                .map(inventoryCommandHandler::handle)
                .toList();
    }
}
