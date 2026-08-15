package com.flab.orderplatform.inventory.domain;

import com.flab.orderplatform.shared.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.LAZY;

/***
 * 재고 변경 히스토리
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "inventory_history",
        indexes = {
                @Index(name = "idx_inventory_history_order_number", columnList = "order_number")
        })
public class InventoryHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;


    @Column(name = "order_number", length = 36, nullable = false, columnDefinition = "VARCHAR(36)  NOT NULL COMMENT '주문번호 (대외 노출용 비즈니스 키)'")
    private String orderNumber;

    @Column(name = "quantity", nullable = false, columnDefinition = "INT NOT NULL COMMENT '변경수량'")
    private Integer quantity;

    @Builder
    public InventoryHistory(String orderNumber, Integer quantity) {
        this.orderNumber = orderNumber;
        this.quantity = quantity;
    }

    public InventoryHistory setInventory(Inventory inventory) {
        this.inventory = inventory;
        return this;
    }
}
