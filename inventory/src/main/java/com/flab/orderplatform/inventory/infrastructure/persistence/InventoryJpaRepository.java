package com.flab.orderplatform.inventory.infrastructure.persistence;

import com.flab.orderplatform.inventory.domain.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;

public interface InventoryJpaRepository extends JpaRepository<Inventory, Long> {
    Set<Inventory> findByProductCodeIn(Set<String> productCodes);

    Optional<Inventory> findByProductCode(String productCode);
}
