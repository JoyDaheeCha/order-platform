package com.flab.orderplatform.inventory.infrastructure.persistence;

import com.flab.orderplatform.inventory.application.port.out.InventoryRepository;
import com.flab.orderplatform.inventory.domain.Inventory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class InventoryRepositoryAdaptor implements InventoryRepository {

    private final InventoryJpaRepository inventoryJpaRepository;
    private final InventoryPessimisticLockStrategy inventoryPessimisticLockStrategy;

    @Override
    public Set<Inventory> findByProductCodeIn(Set<String> productCodes) {
        return inventoryJpaRepository.findByProductCodeIn(productCodes);
    }

    /**
     * 비관락을 사용하여 조회.
     * 데드락을 막기위해, 정렬하여 조회한다.
     *
     * @param productCodes 상품 코드 목록
     * @return 재고 목록
     */
    @Override
    public List<Inventory> findByProductCodeInWithLock(List<String> productCodes) {
        var sortedProductCodes = productCodes.stream().sorted().toList(); // 상품 코드 오름차순 정렬
        return inventoryPessimisticLockStrategy.findByProductCodeIn(sortedProductCodes);
    }

    @Override
    public List<Inventory> saveAll(List<Inventory> inventories) {
        return inventoryJpaRepository.saveAll(inventories);
    }
}
