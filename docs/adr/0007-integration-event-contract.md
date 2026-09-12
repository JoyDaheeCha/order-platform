# ADR-0007: shared 이벤트 계약 — 이벤트 목록 · 이벤트 공통 규약 · 토픽 · 파티션

- **상태**: Accepted (2026-06-27)
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

| 발행 컨텍스트 | 이벤트                          | payload 필드 |
|---------------|------------------------------|--------------|
| **Order** | `OrderCreated`               | `orderNumber`, `buyerId`, `orderItems:[{productId, quantity, unitPrice}]`, `totalAmount` |
| | `OrderPaid`                  | `orderNumber`, `orderItems:[{productCode, quantity}]` |
| | `OrderConfirmed`             ||
| | `OrderCancellationRequested` | `reason`(USER_CANCEL \| TIMEOUT) |
| | `OrderCancelled`             | `reason` (terminal) |
| **Payment** | `PaymentCompleted`           | `paymentId`, `amount` |
| | `PaymentFailed`              | `reason` |
| | `PaymentRefunded`            | `paymentId`, `amount` |
| **Inventory** | `InventoryDecreased`         | `orderItems:[{productId, quantity}]` |
| | `InventoryRestored`          | `orderItems:[{productId, quantity}]` |

### 3.1 주문취소 case - Order에서 주문 취소를 한 경우 
**Order가 먼저 취소를 선언하는** 아래 두 경로에 의해 발행된다.

- case 1. PAID 상태에서 구매자의 주문취소
- case 2. 타임아웃(ADR-0003, 데드라인 초과)

주의: `OrderCancelled`는 보상이 *끝난 뒤* 나오는 terminal 이벤트(PS-2)라 트리거로 쓸 수 없다.

### 3.2 주문 취소 흐름 2가지

```
[보상 이벤트로 인한 취소] PaymentFailed ─▶ Order ─▶ OrderCancelled           (E1, 보상 없음)
            StockShortage ─▶ Payment(RefundPayment) ─▶ PaymentRefunded ─▶ Order ─▶ OrderCancelled  (E2)
[Order 개시] OrderCancellationRequested(reason) ─▶ Payment·Inventory 진행분 보상 ─▶ PaymentRefunded/StockRestored ─▶ Order ─▶ OrderCancelled  (E6·타임아웃)
```

> policy §1 이벤트 맵·보상 흐름을 이 결정에 맞춰 갱신한다(Order 이벤트에 `OrderCancellationRequested` 추가).

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
    Long buyerId,
    List<OrderItem> orderItems,
    Long totalAmount
) implements EventContract {}
```

## 5. 결정 - 토픽 네이밍
- 이벤트별 토픽
- 토픽명: `MSG-<EVENT-NAME>`

| 발행 컨텍스트 | 이벤트                          | 토픽                                 |
|---------------|------------------------------|------------------------------------|
| **Order** | `OrderCreated`               | `MSG-ORDER-CREATED`                |
| | `OrderPaid`                  | `MSG-ORDER-PAID`                   |
| | `OrderConfirmed`             | `MSG-ORDER-CONFIRMED`              |
| | `OrderCancellationRequested` | `MSG-ORDER-CANCELLATION-REQUESTED` |
| | `OrderCancelled`             | `MSG-ORDER-CANCELLED`              |
| **Payment** | `PaymentCompleted`           | `MSG-PAYMENT-COMPLETED`            |
| | `PaymentFailed`              | `MSG-PAYMENT-FAILED`               |
| | `PaymentRefunded`            | `MSG-PAYMENT-REFUNDED`             |
| **Inventory** | `InventoryDeducted`          | `MSG-INVENTORY-DECREASED`          |
| | `InventoryRestored`          | `MSG-INVENTORY-RESTORED`           |

---

## 6. 결정 - 파티션 키

### 결정: 파티션 키 = `orderNumber`

> 구현에서는 컨텍스트 범용 이름 `aggregateId`로 명명된다(Payment·Inventory도 같은 메커니즘을 쓰므로).

- 같은 주문의 이벤트는 **한 토픽 내 동일 파티션**에 적재 → 토픽 내 순서 보장.
