package com.flab.orderplatform.inventory.domain;

import com.flab.orderplatform.inventory.domain.type.InventoryUpdateRequestType;
import com.flab.orderplatform.shared.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.flab.orderplatform.inventory.domain.type.InventoryUpdateRequestType.*;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 10, columnDefinition = "VARCHAR(10) NOT NULL COMMENT '재고 변경 유형 RESERVE/DECREASE'")
    private InventoryUpdateRequestType requestType;

    @Builder
    public InventoryHistory(String orderNumber,
                            Integer quantity,
                            InventoryUpdateRequestType requestType) {
        this.orderNumber = orderNumber;
        this.quantity = quantity;
        this.requestType = requestType;
    }

    public InventoryHistory setInventory(Inventory inventory) {
        this.inventory = inventory;
        return this;
    }

    /**
     * 재고 차감 이력 생성
     *
     * @param orderNumber 주문번호
     * @param quantity    실물 재고 차감 수량
     * @return 재고 변경 이력
     */
    public static InventoryHistory createDecreaseHistory(String orderNumber, int quantity) {
        return InventoryHistory.builder()
                .orderNumber(orderNumber)
                .quantity(quantity)
                .requestType(DECREASE)
                .build();
    }

    /**
     * 재고 선점 이력
     *
     * @param orderNumber 주문 번호
     * @param quantity    재고 선점 수량
     * @return 재고 변경 이력
     */
    public static InventoryHistory createReserveHistory(String orderNumber, int quantity) {
        return InventoryHistory.builder()
                .orderNumber(orderNumber)
                .quantity(quantity)
                .requestType(RESERVE)
                .build();
    }

    /**
     * 재고 선점 취소 이력
     *
     * @param orderNumber 주문 번호
     * @param quantity    재고 선점 취소 수량
     * @return 재고 변경 이력
     */
    public static InventoryHistory createRestoreReservationHistory(String orderNumber, int quantity) {
        return InventoryHistory.builder()
                .orderNumber(orderNumber)
                .quantity(quantity)
                .requestType(RESTORE_RESERVATION)
                .build();
    }
}
