package com.flab.orderplatform.inventory.domain;

import com.flab.orderplatform.inventory.domain.exception.InvalidInventoryChangeException;
import com.flab.orderplatform.inventory.domain.exception.InventoryShortageException;
import com.flab.orderplatform.inventory.domain.exception.ReservedInventoryShortageException;
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

    @Column(name = "stock", nullable = false, columnDefinition = "INT NOT NULL COMMENT '가용 재고 수량'")
    private Integer stock;

    @Column(name = "reserved_stock", nullable = false, columnDefinition = "INT NOT NULL COMMENT '선점된 재고 수량'")
    private Integer reservedStock;

    @OneToMany(mappedBy = "inventory", fetch = LAZY, cascade = {PERSIST})
    private List<InventoryHistory> inventoryHistories = new ArrayList<>();

    @Builder
    public Inventory(String productCode,
                     Integer stock,
                     Integer reservedStock,
                     List<InventoryHistory> inventoryHistories) {
        this.productCode = productCode;
        this.stock = stock;
        this.reservedStock = reservedStock;
        this.inventoryHistories = inventoryHistories;
    }

    /**
     * 재고를 선점한다.
     *
     * @param orderNumber 주문번호
     * @param quantity    재고 선점 요청 수량
     * @return 재고
     */
    public Inventory reserveInventory(String orderNumber, int quantity) {
        if (quantity <= 0) {
            throw new InvalidInventoryChangeException("재고 할당시, 요청 수량은 양수만 가능합니다. (요청 수량: %d)".formatted(quantity));
        }
        if (this.stock < quantity) {
            throw new InventoryShortageException(stock, quantity);
        }
        this.stock -= quantity;
        this.reservedStock += quantity;

        var history = InventoryHistory.createReserveHistory(orderNumber, quantity);
        history.setInventory(this);
        this.inventoryHistories.add(history);

        return this;
    }

    /**
     * 가용 재고 감소<br>
     * 선점된 재고만 감소하며, 가용재고는 재고선점시 감소된 상태라 그대로 유지한다.
     *
     * @param orderNumber        주문 번호
     * @param quantityToDecrease 재고 감소 수량
     * @return 재고
     */
    public Inventory decreaseInventory(String orderNumber, int quantityToDecrease) {
        if (quantityToDecrease <= 0) {
            throw new InvalidInventoryChangeException("재고 할당시, 요청 수량은 양수만 가능합니다. (요청 수량: %d)".formatted(quantityToDecrease));
        }
        if (this.reservedStock < quantityToDecrease) {
            throw new ReservedInventoryShortageException(reservedStock, quantityToDecrease);
        }
        this.reservedStock -= quantityToDecrease;

        var history = InventoryHistory.createDecreaseHistory(orderNumber, quantityToDecrease);
        history.setInventory(this);
        this.inventoryHistories.add(history);

        return this;
    }

    /**
     * 선점된 재고 원복
     *
     * @param orderNumber 주문번호
     * @param quantity    원복할 재고 수량
     * @return 재고
     */
    public Inventory restoreReservedInventory(String orderNumber, int quantity) {
        if (quantity <= 0) {
            throw new IllegalStateException("선점 재고 원복시, 요청 수량은 양수만 가능합니다. (요청 수량: %d)".formatted(quantity));
        }
        if (quantity > reservedStock) {
            throw new IllegalStateException("선점된 재고 수량이 원복 요청 수량보다 적어, 원복이 불가합니다. (선점된 재고수량: %d, 요청된 재고 수량 %d)".formatted(this.reservedStock, quantity));
        }
        this.reservedStock -= quantity;
        this.stock += quantity;

        var history = InventoryHistory.createRestoreReservationHistory(orderNumber, quantity);
        history.setInventory(this);
        this.inventoryHistories.add(history);

        return this;
    }

    /**
     * 가용 재고 원복
     *
     * @param orderNumber 주문 번호
     * @param quantity    원복 수량
     * @return 원복된 재고
     */
    public Inventory restoreInventory(String orderNumber, Integer quantity) {
        if (quantity <= 0) {
            throw new IllegalStateException("가용 재고 원복시, 요청 수량은 양수만 가능합니다. (요청 수량: %d)".formatted(quantity));
        }

        this.stock += quantity;

        var history = InventoryHistory.createRestoreInventoryHistory(orderNumber, quantity);
        history.setInventory(this);
        this.inventoryHistories.add(history);

        return this;
    }
}
