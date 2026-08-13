package com.flab.orderplatform.inventory.infrastructure.lockhandler;

import com.flab.orderplatform.inventory.application.annotation.InventoryTransactional;
import com.flab.orderplatform.inventory.application.command.InventoryDecreaseCommand;
import com.flab.orderplatform.inventory.application.exception.InventoryNotFoundException;
import com.flab.orderplatform.inventory.application.port.out.InventoryRepository;
import com.flab.orderplatform.inventory.domain.Inventory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InventoryDecreaseTransactionalWorker {
    private final InventoryRepository inventoryRepository;

    @InventoryTransactional
    List<Inventory> handle(List<InventoryDecreaseCommand> commands) {
        var productCodes = getProductCodes(commands);
        // 변경 대상 검색
        var inventories = inventoryRepository.findByProductCodeIn(productCodes);
        var inventoryByProductCode = inventories.stream()
                .collect(Collectors.toMap(Inventory::getProductCode, i -> i));

        // 유효성 검증
        validateIfAllInventoryExisting(inventoryByProductCode.keySet(), productCodes);

        var decreasedStocks = commands.stream().map(command -> {
                    var inventory = inventoryByProductCode.get(command.product().productCode());
                    return command.decreaseStock(inventory);
                })
                .toList();
        // version 필드 충돌시 낙관락 발생
        return inventoryRepository.saveAll(decreasedStocks);
    }

    // TODO 락 구현체와 분리하는게 나을지 고민해보기.
    /**
     * 모두 존재하는 재고인지 유효성 검증
     *
     * @param inventoryProductCodeSet 재고 시스템 내 상품 코드 목록
     * @param productCodes            재고 감소 요청된 상푸 코드 목록
     */
    private void validateIfAllInventoryExisting(Set<String> inventoryProductCodeSet, Set<String> productCodes) {
        var productCodeSet = new HashSet<>(productCodes);

        if (!inventoryProductCodeSet.containsAll(productCodeSet)) {
            var missing = productCodeSet
                    .stream()
                    .filter(productCode -> !inventoryProductCodeSet.contains(productCode))
                    .toList();
            throw new InventoryNotFoundException(missing);
        }
    }

    private Set<String> getProductCodes(List<InventoryDecreaseCommand> commands) {
        return commands.stream()
                .map(x -> x.product().productCode())
                .collect(Collectors.toSet());
    }
}
