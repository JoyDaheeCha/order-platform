package com.flab.orderplatform.inventory.infrastructure.persistence;

import com.flab.orderplatform.inventory.application.port.out.InventoryHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InventoryHistoryRepositoryAdaptor implements InventoryHistoryRepository {

    private final InventoryHistoryJpaRepository inventoryHistoryJpaRepository;

    @Override
    public boolean existsByOrderNumber(String orderNumber) {
        return inventoryHistoryJpaRepository.existsByOrderNumber(orderNumber);
    }
}
