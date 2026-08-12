package com.flab.orderplatform.inventory.application.port.out;

import com.flab.orderplatform.inventory.domain.Inventory;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface InventoryRepository {
    Set<Inventory> findByProductCodeIn(Set<String> productCodes);
    Optional<Inventory> findByProductCode(String productCode);
    Inventory save(Inventory decreasedStock);
    List<Inventory> findByProductCodeInWithLock(List<String> productCodes);

    List<Inventory> saveAll(List<Inventory> decreasedStocks);
}
