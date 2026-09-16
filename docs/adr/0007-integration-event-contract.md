# ADR-0007: shared 이벤트 계약 — 이벤트 목록 · 이벤트 공통 규약 · 토픽 · 파티션

- **상태**: Accepted (2026-06-27, 2026-09-16 구현 반영 갱신)
---

## 1. 맥락 (Context)

코레오그래피에서 컨텍스트 간 유일한 결합점은 **`shared`의 이벤트 계약**이다
모든 Kafka 발행·구독 코드가 이 계약에 의존하므로, 코드를 내리기 전에 **이벤트 목록·필드·메타 구조·토픽·파티션 키·버저닝 규약**을 확정한다.

> `shared`엔 **이벤트만** 둔다.
---

## 2. 결정 기준 (Decision Drivers)
**도메인 격리** — payload가 메타·전송포맷으로부터 분리되는가
---

## 3. 결정: 이벤트 목록 · 필드

- `shared`는 통합 이벤트만 정의한다.
- 각 이벤트는 **Kafka 헤더+ payload**로 구성되며, 아래는 payload 비즈니스 필드
- `구현` 열이 ⏳인 이벤트는 `EventConstants`에 이름·토픽만 정의되어 있고, 실제 발행/구독 코드는 아직 없다 (설계상 예약된 이벤트).

| 발행 컨텍스트 | 이벤트                          | payload 필드 | 구현 |
|---------------|------------------------------|--------------|------|
| **Order** | `OrderCreated`               | `orderNumber`, `orderItems:[{productCode, quantity}]` | ✅ |
| | `OrderPaymentPrepared`       | `orderNumber`, `buyerId`, `amount` | ✅ |
| | `OrderPaid`                  | `orderNumber`, `orderItems:[{productCode, quantity}]` | ✅ |
| | `OrderFailed`                | `orderNumber`, `orderItems:[{productCode, quantity}]` | ✅ |
| | `OrderCancellationRequested` | `reason`(USER_CANCEL \| TIMEOUT) | ⏳ |
| **Payment** | `PaymentCompleted`           | `orderNumber`, `pgTid`, `amount` | ✅ |
| | `PaymentFailed`              | `orderNumber` | ✅ |
| **Inventory** | `InventoryReserved`          | `orderNumber`, `reservedAt` | ✅ |
| | `InventoryReservationFailed` | `orderNumber` | ✅ |
| | `IventoryDecreased`          | `orderNumber` | ✅  |

### 3.1 현재 구현된 흐름 (happy path / 실패)
```
[정상] OrderCreated ─▶ Inventory(선점) ─▶ InventoryReserved ─▶ Order(결제요청) ─▶ OrderPaymentPrepared
      ─▶ Payment(결제시도) ─▶ PaymentCompleted ─▶ Order(결제완료) ─▶ OrderPaid ─▶ Inventory(차감) ─▶ StockDeducted (컨슈머 미구현)

[실패] InventoryReservationFailed ─▶ Order: status = ORDER_FAILED (재고부족)
      PaymentFailed              ─▶ Order: status = ORDER_FAILED (결제실패)
      재고 선점 후 10분 타임아웃    ─▶ Order: status = ORDER_FAILED (타임아웃, ADR-0003)
```
- 모든 실패 경로는 `Order`가 `OrderFailed`를 발행하는 것으로 끝난다. Inventory 컨텍스트의 `OrderFailedEventProcessor`가 이를 컨슈밍해 `Inventory.restoreReservedInventory()`로 선점 수량을 원복한다 (별도 `StockRestored` 이벤트 발행 없이 `OrderFailed`를 직접 구독).
- 결제 환불(`PaymentRefunded`)은 아직 구현되어 있지 않다 — 현재 실패 경로는 모두 `PENDING_PAYMENT` 단계(재고선점 완료~결제 전/결제 실패)에서 끝나므로 결제가 완료된 뒤 취소하는 케이스(환불 대상)가 아직 없다.

---

## 4. 결정: shared 내 메시지 포멧
메타는 Kafka 헤더 + 메시지 키로, body는 payload record 단독
```
Kafka 헤더  ──▶ eventId · eventType · aggregateType · occurredAt
message key ──▶ aggregateId
body(JSON)  ──▶ shared 의 payload record 단독
```

```java
public interface EventContract {   // shared/event/EventContract.java
    String eventType();  // 'OrderCreated'
    String topic();      // 'MSG-ORDER-CREATED'
}

public record OrderCreatedPayload(
    String orderNumber,
    List<OrderItemDto> orderItems
) implements EventContract {}
```

## 5. 결정 - 토픽 네이밍
- 이벤트별 토픽
- 토픽명: `MSG-<EVENT-NAME>`

| 발행 컨텍스트 | 이벤트 | 토픽 | 구현 |
|---------------|--------|------|------|
| **Order** | `OrderCreated` | `MSG-ORDER-CREATED` | ✅ |
| | `OrderPaymentPrepared` | `MSG-ORDER-PAYMENT-PREPARED` | ✅ |
| | `OrderPaid` | `MSG-ORDER-PAID` | ✅ |
| | `OrderFailed` | `MSG-ORDER-FAILED` | ✅ |
| | `OrderConfirmed` | `MSG-ORDER-CONFIRMED` | ⏳ |
| | `OrderCancellationRequested` | `MSG-ORDER-CANCELLATION-REQUESTED` | ⏳ |
| | `OrderCancelled` | `MSG-ORDER-CANCELLED` | ⏳ |
| **Payment** | `PaymentCompleted` | `MSG-PAYMENT-COMPLETED` | ✅ |
| | `PaymentFailed` | `MSG-PAYMENT-FAILED` | ✅ |
| | `PaymentRefunded` | `MSG-PAYMENT-REFUNDED` | ⏳ |
| **Inventory** | `InventoryReserved` | `MSG-INVENTORY-RESERVED` | ✅ |
| | `InventoryReservationFailed` | `MSG-INVENTORY-RESERVATION-FAILED` | ✅ |
| | `StockDeducted` | `MSG-STOCK-DEDUCTED` | ✅ (구독 컨슈머 없음) |
| | `StockShortage` | `MSG-STOCK-SHORTAGE` | ⏳ |
| | `StockRestored` | `MSG-STOCK-RESTORED` | ⏳ |

---

## 6. 결정 - 파티션 키

### 결정: 파티션 키 = `orderNumber`

> 구현에서는 컨텍스트 범용 이름 `aggregateId`로 명명된다(Payment·Inventory도 같은 메커니즘을 쓰므로).

- 같은 주문의 이벤트는 **한 토픽 내 동일 파티션**에 적재 → 토픽 내 순서 보장.
