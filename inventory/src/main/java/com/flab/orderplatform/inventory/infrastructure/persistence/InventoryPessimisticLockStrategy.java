package com.flab.orderplatform.inventory.infrastructure.persistence;

import com.flab.orderplatform.inventory.domain.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InventoryPessimisticLockStrategy extends JpaRepository<Inventory, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.productCode in :productCodes")
    List<Inventory> findByProductCodeIn(List<String> productCodes);
}
