# ADR-0003: 주문 타임아웃 · 예외처리방법

- **상태**: Accepted (2026-06-27, 2026-09-16 구현 반영 갱신)

---

## 1. 맥락

- 코레오그래피에는 "결제 이벤트가 안 온다"를 지켜보는 중앙감시자가 없다.
- 재고 선점(`RESERVING_INVENTORY` → `PENDING_PAYMENT`) 이후 결제가 끝나지 않으면, 선점된 재고를 무기한 점유하게 된다.

## 2. 요구사항과 구현법
### 2.1 요구사항
- 재고 선점 후 일정 시간 내 결제가 완료되지 않으면 실패로 간주하고 선점을 해제한다.
- 비즈니스 예외와 인프라 이슈로 인한 일시적 예외를 구분지어 처리해야한다 (예외 타입에 따라 보상 이벤트 발행할지, 아니면 재시도할지 결정필요)

### 2.2 구현방법
- 타임아웃
  - `orders` 테이블에 재고 선점 정보(`reserved_at`·`is_released`·`reservation_release_reason`)를 둔다.
  - 스케줄러로 폴링하며 타임아웃 여부 체크 (주문 컨텍스트)
- 예외
  - 인프라: 인박스(Inbox) 이벤트 처리 중 예외가 나면 상태를 `FAILED`로 남기고, 다음 폴링 주기에 `CREATED`와 함께 재조회되어 재시도한다.
  - 비즈니스: 예외를 던지지 않고, 실패를 나타내는 별도 이벤트를 발행해 즉시 보상(주문 실패) 처리로 이어간다.

---

## 3. 결정: 주문타임아웃 확인 로직
### 3.1 타임아웃 기준
- `PENDING_PAYMENT`(결제 대기중) 상태로 재고를 선점한 주문이, 선점 시각(`reserved_at`)으로부터 **10분**이 지나도록 결제가 완료되지 않으면 타임아웃으로 간주한다.

### 3.2 타임아웃 구현 방법
- `orders` 테이블 (`Order` 애그리거트 루트 + `OrderInventoryReservation` 임베디드 컬럼)
```
orders
  order_number                VARCHAR(36)  -- PK(대외 노출용 비즈니스 키)
  status                      VARCHAR(20)  -- RESERVING_INVENTORY/PENDING_PAYMENT/PAID/CONFIRMED/CANCELLED/ORDER_FAILED
  reserved_at                 DATETIME(6)  -- 재고 선점 일시 (PENDING_PAYMENT 진입 시점)
  is_released                 TINYINT(1)   -- 재고 선점 해제 여부
  reservation_release_reason  VARCHAR(30)  -- TIMEOUT / PAYMENT_COMPLETED / PAYMENT_FAILED
  order_failed_reason         VARCHAR(30)  -- INVENTORY_SHORTAGE / TIMEOUT / PAYMENT_FAILED
```

### 3.3 구현시 주의사항

- **정상 이벤트와 타임아웃 동시발생 가능성**
  - 스케줄러가 릴리스 대상으로 조회한 직후 `PaymentCompleted`가 도착할 수 있다.
  - `pay()`와 `failByTimeout()` 모두 `this.status`가 기대 상태가 아니면 예외를 던지는 상태 가드로 경합을 방지한다 (`pay()`는 이미 `PAID`인 경우 멱등하게 무시하고 그대로 반환).
  - `ShedLock`(`@SchedulerLock`)으로 여러 인스턴스가 동시에 같은 스케줄러를 실행하는 것 자체를 막는다.

### 3.4 타임아웃 발생시 정책: 주문 실패 처리

`Order.failByTimeout()`이 한 트랜잭션 안에서 아래를 수행한다.
```
1. status = ORDER_FAILED, order_failed_reason = TIMEOUT
2. 재고 선점 해제 (is_released = true, reservation_release_reason = TIMEOUT)
3. OrderFailed 이벤트 발행 (Outbox, ADR-0007)
```
> Inventory 컨텍스트의 `OrderFailedEventProcessor`가 `OrderFailed`를 컨슈밍해 `Inventory.restoreReservedInventory()`로 선점 수량(`reservedStock`)을 가용재고(`stock`)로 되돌린다 — 재고부족/결제실패로 인한 `OrderFailed`도 동일 컨슈머가 처리한다.

---

## 4. 결정 : 비즈니스 실패와 인프라 오류 처리방법

### 4.1 처리방법

| 부류 | 예 | 처리 |
|------|-----|------|
| **비즈니스 실패** | 재고부족·결제거절 | 재시도 X → 예외를 던지지 않고 실패 이벤트(`InventoryReservationFailed`/`PaymentFailed`)를 발행 → 보상 |
| **일시적 인프라 오류** | DB 순단, 네트워크 타임아웃 | 인박스 이벤트 처리 실패시 `InboxEvent.status = FAILED`로 기록 → 1초 간격 폴링에서 `CREATED`와 함께 재조회되어 재시도 (DLQ·별도 실패 로그 테이블 없음) |