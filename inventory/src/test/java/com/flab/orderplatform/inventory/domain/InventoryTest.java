package com.flab.orderplatform.inventory.domain;

import com.flab.orderplatform.inventory.domain.exception.InvalidInventoryChangeException;
import com.flab.orderplatform.inventory.domain.exception.InventoryShortageException;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static com.flab.orderplatform.inventory.domain.type.InventoryUpdateRequestType.DECREASE;
import static com.flab.orderplatform.inventory.domain.type.InventoryUpdateRequestType.RESERVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("재고 단위 테스트")
class InventoryTest {

    private static final String ORDER_NUMBER = "20260730-8N4ZLC2RPD";

    @DisplayName("성공: 재고 선점에 성공시, 가용 재고는 줄어들고 선점 재고는 늘어난다")
    @Test
    void reserveInventory() {
        // given
        // TODO fixture monkey로 픽스처 대체
        var inventory = Inventory.builder()
                .productCode("GD10001")
                .stock(100)
                .reservedStock(0)
                .inventoryHistories(new ArrayList<>())
                .build();
        var previousReservedInventoryCount = inventory.getReservedStock();
        var previousInventoryCount = inventory.getStock();

        // when
        var reservationCount = 20;
        var updatedInventory = inventory.reserveInventory(ORDER_NUMBER, reservationCount);

        // then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(previousReservedInventoryCount).isEqualTo(0);
            softly.assertThat(previousInventoryCount).isEqualTo(100);
            softly.assertThat(updatedInventory.getReservedStock()).isEqualTo(20);
            softly.assertThat(updatedInventory.getStock()).isEqualTo(80);
        });
    }

    @DisplayName("성공: 재고 선점에 성공시, 재고 선점 이력이 생성된다.")
    @Test
    void reserveInventoryHistory() {
        // given
        var inventory = Inventory.builder()
                .productCode("GD10001")
                .stock(100)
                .reservedStock(0)
                .inventoryHistories(new ArrayList<>())
                .build();

        // when
        var reservationCount = 20;
        var updatedInventory = inventory.reserveInventory(ORDER_NUMBER, reservationCount);

        // then
        assertThat(updatedInventory.getInventoryHistories().getFirst())
                .extracting ("orderNumber", "quantity", "requestType")
                .containsExactly(ORDER_NUMBER, 20, RESERVE);
    }

    @DisplayName("예외: 재고 선점 요청 수량이 0미만이면 선점 실패한다.")
    @Test
    void reserveNegativeInventory() {
        // given
        var inventory = Inventory.builder()
                .productCode("GD10001")
                .stock(100)
                .reservedStock(0)
                .inventoryHistories(new ArrayList<>())
                .build();
        var invalidQuantity = -1;  // 재고 선점 요청 수량이 음수

        // when & then
        assertThrows(InvalidInventoryChangeException.class,
                () -> inventory.reserveInventory(ORDER_NUMBER, invalidQuantity));
    }

    @DisplayName("예외: 재고 선점 요청 수량이 가용 재고보다 크면 선점에 실패한다")
    @Test
    void reserveInventoryForLessStock() {
        // given
        var inventory = Inventory.builder()
                .productCode("GD10001")
                .stock(10)
                .reservedStock(0)
                .inventoryHistories(new ArrayList<>())
                .build();
        var invalidQuantity = 20;

        // when & then
        assertThrows(InventoryShortageException.class,
                () -> inventory.reserveInventory(ORDER_NUMBER, invalidQuantity));
    }

    @DisplayName("성공: 재고 감소 성공시, (1) 선점수량은 줄어들고, (2) 가용 재고 수량은 줄어들지 않으며 (3)재고 감소 이력이 생성된다")
    @Test
    void decreaseInventory() {
        // given
        var inventory = Inventory.builder()
                .productCode("GD10001")
                .stock(70)
                .reservedStock(50)
                .inventoryHistories(new ArrayList<>())
                .build();

        // when
        var decreaseQuantity = 20;
        var updatedInventory = inventory.decreaseStock(ORDER_NUMBER, decreaseQuantity);

        // then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(updatedInventory.getReservedStock()).isEqualTo(30);
            softly.assertThat(updatedInventory.getStock()).isEqualTo(70);
            softly.assertThat(updatedInventory.getInventoryHistories().getFirst()).extracting ("orderNumber", "quantity", "requestType")
                    .containsExactly(ORDER_NUMBER, 20, DECREASE);
        });
    }
}
