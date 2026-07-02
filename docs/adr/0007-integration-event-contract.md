# ADR-0007: shared 통합 이벤트 계약 — 이벤트 목록 · Envelope · 토픽 · 파티션 · 버저닝

- **상태(Status)**: Accepted (2026-06-27)
- **관련 정책**: [policy.md](../policy.md) §1(이벤트 맵), §3(PI-4·5·6) / [architecture.md](../architecture.md) §3(C-2·C-3·C-4), §4(shared 모듈)
- **선행 결정**: [ADR-0001](./0001-saga-orchestration-vs-choreography.md) 코레오그래피, [ADR-0004](./0004-schema-separation-outbox-readmodel.md) Outbox/Inbox(payload JSON, envelope 컬럼 구조), [ADR-0003](./0003-order-deadline-checker.md) 타임아웃 → 보상-개시
- **후속 의존**: ADR-0006(인바운드 응답·멱등키) — **클라이언트 멱등키는 이 이벤트 계약에 포함하지 않는다**(경계 분리, §3 주의)

---

## 1. 맥락 (Context)

코레오그래피(ADR-0001)에서 컨텍스트 간 유일한 결합점은 **`shared`의 통합 이벤트 계약**이다(architecture C-3·C-5). 모든 Kafka 발행·구독 코드가 이 계약에 의존하므로, 코드를 내리기 전에 **이벤트 목록·필드·메타 구조·토픽·파티션 키·버저닝 규약**을 확정한다.

> `shared`엔 **이벤트만** 둔다. `ProcessPayment`·`DeductStock` 등 커맨드는 외부 메시지가 아니라 이벤트 수신 시 컨텍스트 내부에서 실행되는 동작이다(policy §1 주석). 외부 커맨드는 구매자발 `PlaceOrder`·`CancelOrder`뿐이며 이는 인바운드 API(ADR-0006)이지 통합 이벤트가 아니다.

선행 제약: C-4(도메인 이벤트 ≠ 통합 이벤트), PI-4(모든 이벤트는 `eventId`·`occurredAt`·`orderId` 필수).

---

## 2. 결정 기준 (Decision Drivers)

| # | 기준 | 가중치 |
|---|------|--------|
| D1 | **도메인 격리** — payload가 메타·전송포맷으로부터 분리되는가(C-4) | 높음 |
| D2 | **Outbox/Inbox 정합** — ADR-0004 테이블 구조와 어긋나지 않는가 | 높음 |
| D3 | **보상 흐름 완전성** — 코레오그래피의 모든 실패·취소 경로가 이벤트로 닫히는가 | 높음 |
| D4 | **단순성** — 학습 범위에서 토픽·스키마 관리가 과하지 않은가 | 중간 |

---

## 3. 결정 A — 이벤트 목록 · 필드

`shared`는 통합 이벤트만 정의한다. 각 이벤트는 **envelope 메타(§4) + payload**로 구성되며, 아래는 payload 비즈니스 필드(메타 `eventId`/`occurredAt`/`orderId`는 envelope에 있음).

| 발행 컨텍스트 | 이벤트 | payload 필드 |
|---------------|--------|--------------|
| **Order** | `OrderPlaced` | `buyerId`, `lines:[{productId, quantity, unitPrice}]`, `totalAmount` |
| | `OrderConfirmed` | — (orderId는 envelope) |
| | `OrderCancellationRequested` | `reason`(USER_CANCEL \| TIMEOUT) ★ 신설 |
| | `OrderCancelled` | `reason` (terminal) |
| **Payment** | `PaymentCompleted` | `paymentId`, `amount` |
| | `PaymentFailed` | `reason` |
| | `PaymentRefunded` | `paymentId`, `amount` |
| **Inventory** | `StockDeducted` | `lines:[{productId, quantity}]` |
| | `StockShortage` | `shortageProductIds:[]` |
| | `StockRestored` | `lines:[{productId, quantity}]` |

### A-1. 보상-개시 이벤트 `OrderCancellationRequested` 신설 (D3 핵심)

policy §1의 보상 흐름은 *하류 실패가 보상을 끄는* 경로(E1 결제실패·E2 재고부족)만 그려져 있었다. 그러나 **Order가 먼저 취소를 선언하는** 두 경로 —

- **E6 사용자 취소(PAID 상태, PC-3)**
- **타임아웃(ADR-0003, 데드라인 초과)**

— 는 코레오그래피상 "Order → (Payment/Inventory가 구독해 진행분 보상)"를 끌 **트리거 이벤트**가 필요하다. `OrderCancelled`는 보상이 *끝난 뒤* 나오는 terminal 이벤트(PS-2)라 트리거로 쓸 수 없다.

**결정**: 보상-개시를 **단일 이벤트 `OrderCancellationRequested(reason)`** 로 통합한다.
- Payment/Inventory가 구독 → 진행분만 역순 보상(PB-1, PB-2 멱등) → `PaymentRefunded`/`StockRestored` 발행 → Order가 수신·집계 → `OrderCancelled`(terminal).
- **대안 기각**(타임아웃용·사용자취소용 이벤트를 따로): 구독자가 두 이벤트에 동일 반응을 해야 해 중복. `reason` 필드로 통합하는 편이 단순(D4).
- ADR-0003의 `OrderTimedOut`은 이 이벤트의 `reason=TIMEOUT` 케이스로 흡수한다.

### A-2. 보상 흐름 (갱신)

```
[하류 실패] PaymentFailed ─▶ Order ─▶ OrderCancelled           (E1, 보상 없음)
            StockShortage ─▶ Payment(RefundPayment) ─▶ PaymentRefunded ─▶ Order ─▶ OrderCancelled  (E2)
[Order 개시] OrderCancellationRequested(reason) ─▶ Payment·Inventory 진행분 보상 ─▶ PaymentRefunded/StockRestored ─▶ Order ─▶ OrderCancelled  (E6·타임아웃)
```

> policy §1 이벤트 맵·보상 흐름을 이 결정에 맞춰 갱신한다(Order 이벤트에 `OrderCancellationRequested` 추가).

---

## 4. 결정 B — Envelope (봉투) 구조

PI-4 메타(`eventId`·`occurredAt`·`orderId`)를 담는 방식.

### 고려한 옵션

| 옵션 | 평가 |
|------|------|
| (a) 평면 — 모든 record가 메타 3필드 보유 | 중복·누락 위험(D1 △) |
| (b) 인터페이스 `IntegrationEvent` 구현 | record라 필드는 여전히 각자 보유 |
| **(c) 봉투(composition)** ✅ | 메타와 payload를 타입으로 분리 |

### 결정: (c) `EventEnvelope<T>` 채택

```java
record EventEnvelope<T>(
    UUID eventId,        // PI-4 멱등키 → 소비자 Inbox 키(PI-5)
    Instant occurredAt,
    String orderId,      // PI-4 상관관계·파티션 키
    String eventType,    // 'OrderPlaced' 등 (구독자 분기·역직렬화 디스패치)
    T payload            // 도메인별 비즈니스 record (메타를 모름)
) {}
```

근거 — **ADR-0004 Outbox 테이블 구조와 1:1 일치**(D2 ◎):

```
Outbox 컬럼:   event_id  occurred_at  aggregate_id  event_type  payload(JSON)
EventEnvelope: eventId   occurredAt   orderId       eventType   payload(T)
```

- payload record(`OrderPlaced` 등)는 메타·토픽·직렬화를 모른다 → C-4(도메인 격리, D1 ◎) 자연 충족. infra 어댑터가 도메인 이벤트 → payload → envelope 포장 → Outbox 적재.
- `eventType`은 단일 토픽에 섞인 이벤트의 **구독 분기 + 역직렬화 디스패치** 키(§5와 연계).

---

## 5. 결정 C — 토픽 네이밍

| 옵션 | 토픽 수 | 평가 |
|------|---------|------|
| 이벤트별 토픽(`order.placed` …) | 9+ | 토픽 폭발, 순서 단위 파편화(D4 ✗) |
| **컨텍스트(Aggregate)별 토픽** ✅ | 3 | `order.events`·`payment.events`·`inventory.events` |

### 결정: `<context>.events` 3토픽

- 한 Aggregate의 이벤트 스트림 = 한 토픽 → "한 주문의 이벤트가 한 파티션에 순서대로"가 자연스럽게 성립(§6).
- 구독자는 envelope `eventType`으로 분기. 토픽 수 최소로 운영·구성 단순(D4 ◎).
- 트레이드오프: 구독자가 관심 없는 타입도 수신해 필터링(미미한 비용).

---

## 6. 결정 D — 파티션 키 / 순서 보장

### 결정: 파티션 키 = `orderId` (policy PI-4 확정 재확인)

- 같은 주문의 이벤트는 **한 토픽 내 동일 파티션**에 적재 → 토픽 내 순서 보장.
- **학습 포인트(명시)**: 순서 보장은 *토픽-파티션 내*에서만 성립. 한 주문의 이벤트가 3개 토픽에 흩어지므로 **토픽 간 전역 순서는 보장되지 않는다.** 이는 문제가 아니다 — 인과 순서는 **사가 흐름 자체가 강제**한다(Inventory는 `PaymentCompleted`를 받아야만 차감). 코레오그래피가 토픽 간 전역 순서에 *의존하면 안 되는* 이유의 사례.

---

## 7. 결정 E — 버저닝 / 스키마 진화

### 결정: 가산적(additive-only) 규약, Schema Registry 없음

- payload 직렬화 = **JSON**(Outbox payload가 JSON, ADR-0004).
- **규약**: 필드 *추가*만 허용(하위호환). *삭제·의미 변경*은 금지 — 정말 깨지는 변경은 **새 `eventType`** 으로 신설.
- Schema Registry(Avro/Confluent)는 학습 범위에 오버스펙 → **도입하지 않음**(필요 시 후속 학습).
- envelope `schemaVersion` 필드는 **보류** — 가산 규약으로 충분, 필요해질 때 추가.

---

## 8. 결과 / 영향 (Consequences)

**긍정**
- 컨텍스트 간 계약이 `shared` 한 곳에 모이고, payload는 메타·전송포맷과 분리(C-4, D1).
- envelope가 Outbox/Inbox 컬럼과 일치 → 포장/역포장이 자명(D2).
- 보상-개시 이벤트 신설로 **모든 실패·취소 경로가 이벤트로 닫힘**(D3) — 코레오그래피의 보상 흐름 공백 제거.

**부정 / 주의**
- 단일 토픽에 여러 `eventType`이 섞여 구독자가 **필터·디스패치** 로직을 가져야 한다(envelope `eventType` 의존).
- 가산 규약은 *규율*이라 컴파일러가 강제 못 함 — 호환성 위반은 리뷰로 막는다.
- payload JSON은 스키마 강제가 약함(레지스트리 없음) — 학습 범위의 의도적 단순화.

**다른 문서 영향**
- **policy.md §1**: Order 이벤트에 `OrderCancellationRequested` 추가, 보상 흐름에 Order-개시 경로 반영.
- **ADR-0003**: `OrderTimedOut` → `OrderCancellationRequested(reason=TIMEOUT)`로 정합.

---

## 9. 미해결 (이 ADR 범위 밖)

- 클라이언트 멱등키 저장·인바운드 응답 모델 → **ADR-0006**(별도 세션). 멱등키는 이 계약에 포함하지 않는다(경계).
- Inbox 키 `event_id` 단독 vs `(event_id, handler)` 복합 → ADR-0004 §7, 구독 토폴로지 확정 시. (단일 토픽·다구독 구조에서 한 컨텍스트가 같은 이벤트를 여러 핸들러로 처리하면 복합키 필요.)
- 토픽 파티션 수·리텐션·컨슈머 그룹 구성 — 운영 튜닝(코드 착수 시).
- `schemaVersion`/레지스트리 도입 — 호환성 관리가 필요해지는 후속 학습.
