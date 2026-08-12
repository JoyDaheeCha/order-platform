package com.flab.orderplatform.inventory.infrastructure.persistence;

import com.flab.orderplatform.inventory.application.port.out.InventoryRepository;
import com.flab.orderplatform.inventory.domain.Inventory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class InventoryRepositoryAdaptor implements InventoryRepository {

    private final InventoryJpaRepository inventoryJpaRepository;

    @Override
    public Set<Inventory> findByProductCodeIn(Set<String> productCodes) {
        return inventoryJpaRepository.findByProductCodeIn(productCodes);
    }

    @Override
    public Optional<Inventory> findByProductCode(String productCode) {
        return inventoryJpaRepository.findByProductCode(productCode);
    }

    @Override
    public Inventory save(Inventory inventory) {
        return inventoryJpaRepository.save(inventory);
    }
}
