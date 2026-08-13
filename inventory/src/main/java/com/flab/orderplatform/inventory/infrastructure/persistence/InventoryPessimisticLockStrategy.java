package com.flab.orderplatform.inventory.infrastructure.persistence;

import com.flab.orderplatform.inventory.domain.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface InventoryPessimisticLockStrategy extends JpaRepository<Inventory, Long> {

    /**
     * 재고 감소시 비관락 적용 (타임아웃 3초)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.productCode in :productCodes order by i.productCode")
    Set<Inventory> findByProductCodeInOrderbyProductCode(@Param("productCodes") Set<String> productCodes);
}
