package com.flab.orderplatform.inventory.infrastructure.persistence;

import com.flab.orderplatform.inventory.domain.InventoryHistory;
import com.flab.orderplatform.inventory.domain.type.InventoryUpdateRequestType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryHistoryJpaRepository extends JpaRepository<InventoryHistory, Long> {
    boolean existsByOrderNumberAndRequestType(String orderNumber, InventoryUpdateRequestType requestType);
}