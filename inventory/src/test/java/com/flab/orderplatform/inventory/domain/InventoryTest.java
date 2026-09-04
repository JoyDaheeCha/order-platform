package com.flab.orderplatform.inventory.domain;

import com.flab.orderplatform.inventory.domain.exception.InvalidInventoryChangeException;
import com.flab.orderplatform.inventory.domain.exception.InventoryShortageException;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("재고 단위 테스트")
class InventoryTest {

    @DisplayName("성공: 재고 선점에 성공시, 가용 재고는 줄어들고 선점 재고는 늘어난다")
    @Test
    void reserveInventory() {
        // given
        // TODO fixture monkey로 픽스처 대체
        var inventory = Inventory.builder()
                .productCode("GD10001")
                .stock(100)
                .reservedStock(0)
                .inventoryHistories(null)
                .build();
        var previousReservedInventoryCount = inventory.getReservedStock();
        var previousInventoryCount = inventory.getStock();

        // when
        var reservationCount = 20;
        var updatedInventory = inventory.reserveInventory(reservationCount);

        // then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(previousReservedInventoryCount).isEqualTo(0);
            softly.assertThat(previousInventoryCount).isEqualTo(100);
            softly.assertThat(updatedInventory.getReservedStock()).isEqualTo(20);
            softly.assertThat(updatedInventory.getStock()).isEqualTo(80);
        });
    }

    @DisplayName("예외: 재고 선점 요청 수량이 0미만이면 선점 실패한다.")
    @Test
    void reserveNegativeInventory() {
        // given
        var inventory = Inventory.builder()
                .productCode("GD10001")
                .stock(100)
                .inventoryHistories(null)
                .build();
        var invalidQuantity = -1;  // 재고 선점 요청 수량이 음수

        // when & then
        assertThrows(InvalidInventoryChangeException.class,
                () -> inventory.reserveInventory(invalidQuantity));
    }

    @DisplayName("예외: 재고 선점 요청 수량이 가용 재고보다 크면 선점에 실패한다")
    @Test
    void reserveInventoryForLessStock() {
        // given
        var inventory = Inventory.builder()
                .productCode("GD10001")
                .stock(10)
                .inventoryHistories(null)
                .build();
        var invalidQuantity = 20;

        // when & then
        assertThrows(InventoryShortageException.class,
                () -> inventory.reserveInventory(invalidQuantity));
    }
}