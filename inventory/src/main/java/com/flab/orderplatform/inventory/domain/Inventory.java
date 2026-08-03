package com.flab.orderplatform.inventory.domain;

import com.flab.orderplatform.inventory.domain.exception.InvalidInventoryChangeException;
import com.flab.orderplatform.inventory.domain.exception.InventoryShortageException;
import com.flab.orderplatform.shared.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.FetchType.LAZY;

/***
 * 재고
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "inventory")
public class Inventory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 36, unique = true, nullable = false,
            columnDefinition = "VARCHAR(36) NOT NULL COMMENT '상품코드 (예. GD10001)'")
    private String productCode;

    @Column(name = "stock", nullable = false, columnDefinition = "INT NOT NULL COMMENT '재고 수량'")
    private Integer stock;

    @OneToMany(mappedBy = "inventory", fetch = LAZY, cascade = {PERSIST})
    private List<InventoryHistory> inventoryHistories = new ArrayList<>();

    @Builder
    public Inventory(String productCode, Integer stock, List<InventoryHistory> inventoryHistories) {
        this.productCode = productCode;
        this.stock = stock;
        this.inventoryHistories = inventoryHistories;
    }

    public Inventory decreaseStock(String orderNumber, int quantityToDecrease) {
        if (quantityToDecrease <= 0) {
            throw new InvalidInventoryChangeException("재고 할당시, 요청 수량은 양수만 가능합니다. (요청 수량: %d)".formatted(quantityToDecrease));
        }
        if (this.stock < quantityToDecrease) {
            throw new InventoryShortageException(stock, quantityToDecrease);
        }
        this.stock -= quantityToDecrease;

        var history = InventoryHistory.builder()
                .orderNumber(orderNumber)
                .quantity(quantityToDecrease)
                .build();

        addInventoryHistory(history);
        return this;
    }

    private void addInventoryHistory(InventoryHistory history) {
        history.setInventory(this);
        this.inventoryHistories.add(history);
    }
}
