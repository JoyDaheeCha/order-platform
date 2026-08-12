package com.flab.orderplatform.inventory.application;

import com.flab.orderplatform.inventory.application.annotation.InventoryTransactional;
import com.flab.orderplatform.inventory.application.command.InventoryDecreaseCommand;
import com.flab.orderplatform.inventory.application.exception.InventoryNotFoundException;
import com.flab.orderplatform.inventory.application.port.out.InventoryDecreaseCommandHandler;
import com.flab.orderplatform.inventory.application.port.out.InventoryRepository;
import com.flab.orderplatform.inventory.domain.Inventory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PessimisticLockInventoryDecreaseCommandHandler implements InventoryDecreaseCommandHandler {
    private final InventoryRepository inventoryRepository;

    /**
     * 특정 주문에 대해 재고를 일괄 감소시킨다.
     */
    @InventoryTransactional
    public List<Inventory> handle(List<InventoryDecreaseCommand> commands) {
        var productCodes = getProductCodes(commands);
        // 비관락 적용
        var inventories = inventoryRepository.findByProductCodeInWithLock(productCodes);
        var inventoryByProductCode = inventories.stream()
                .collect(Collectors.toMap(Inventory::getProductCode, i -> i));

        // 유효성 검증
        validateIfAllInventoryExisting(inventoryByProductCode.keySet(), productCodes);

        var decreasedStocks = commands.stream().map(command -> {
                    var inventory = inventoryByProductCode.get(command.product().productCode());
                    return command.decreaseStock(inventory);
                })
                .toList();
        return inventoryRepository.saveAll(decreasedStocks);
    }

    // TODO 락 구현체와 분리하는게 나을지 고민해보기.
    /**
     * 모두 존재하는 재고인지 유효성 검증
     *
     * @param inventoryProductCodeSet 재고 시스템 내 상품 코드 목록
     * @param productCodes            재고 감소 요청된 상푸 코드 목록
     */
    private void validateIfAllInventoryExisting(Set<String> inventoryProductCodeSet, List<String> productCodes) {
        var productCodeSet = new HashSet<>(productCodes);

        if (!inventoryProductCodeSet.containsAll(productCodeSet)) {
            var missing = productCodeSet
                    .stream()
                    .filter(productCode -> !inventoryProductCodeSet.contains(productCode))
                    .toList();
            throw new InventoryNotFoundException(missing);
        }
    }

    private List<String> getProductCodes(List<InventoryDecreaseCommand> commands) {
        return commands.stream()
                .map(x -> x.product().productCode())
                .sorted() // 상품 코드 오름 차순 정렬. 비관락 적용시 선제 조건
                .toList();
    }
}
