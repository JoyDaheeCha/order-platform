package com.flab.orderplatform.inventory.infrastructure.persistence;

import com.flab.orderplatform.inventory.domain.Inventory;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryPessimisticLockStrategy extends JpaRepository<Inventory, Long> {

    /**
     * 재고 감소시 비관락 적용 (타임아웃 3초)
     */
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")
    })
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.productCode in :productCodes")
    List<Inventory> findByProductCodeIn(@Param("productCodes") List<String> productCodes);
}
