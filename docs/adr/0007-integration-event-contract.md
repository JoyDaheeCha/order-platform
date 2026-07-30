# ADR-0007: shared 통합 이벤트 계약 — 이벤트 목록 · Envelope · 토픽 · 파티션 · 버저닝

- **상태(Status)**: Accepted (2026-06-27) — 개정 2026-07-30: §5 토픽 전략을 컨텍스트별 토픽 → **이벤트별 토픽**으로 변경(§4·§6·§8 정합 반영)
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
| **Order** | `OrderCreated` | `buyerId`, `orderItems:[{productId, quantity, unitPrice}]`, `totalAmount` |
| | `OrderConfirmed` | — (orderId는 envelope) |
| | `OrderCancellationRequested` | `reason`(USER_CANCEL \| TIMEOUT) ★ 신설 |
| | `OrderCancelled` | `reason` (terminal) |
| **Payment** | `PaymentCompleted` | `paymentId`, `amount` |
| | `PaymentFailed` | `reason` |
| | `PaymentRefunded` | `paymentId`, `amount` |
| **Inventory** | `StockDeducted` | `orderItems:[{productId, quantity}]` |
| | `StockShortage` | `shortageProductIds:[]` |
| | `StockRestored` | `orderItems:[{productId, quantity}]` |

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
    String eventType,    // 'OrderCreated' 등 (Outbox event_type과 1:1, 메시지 자기기술)
    T payload            // 도메인별 비즈니스 record (메타를 모름)
) {}
```

근거 — **ADR-0004 Outbox 테이블 구조와 1:1 일치**(D2 ◎):

```
Outbox 컬럼:   event_id  occurred_at  aggregate_id  event_type  payload(JSON)
EventEnvelope: eventId   occurredAt   orderId       eventType   payload(T)
```

- payload record(`OrderCreated` 등)는 메타·토픽·직렬화를 모른다 → C-4(도메인 격리, D1 ◎) 자연 충족. infra 어댑터가 도메인 이벤트 → payload → envelope 포장 → Outbox 적재.
- `eventType`은 토픽(§5)과 정보가 겹치지만, Outbox 컬럼 정합(D2)과 **메시지 자기기술**을 위해 envelope에 남긴다 — 토픽 맥락이 사라진 로그·Inbox 레코드·DLQ에서도 이벤트 정체가 드러나야 한다.

---

## 5. 결정 C — 토픽 네이밍

| 옵션 | 토픽 수 | 평가 |
|------|---------|------|
| 컨텍스트(Aggregate)별 토픽(`order.events` …) | 3 | 구독자가 관심 없는 타입까지 수신 → 필터·디스패치 로직 필요 |
| **이벤트별 토픽** ✅ | 10 | 토픽 = 이벤트 타입 1:1, 구독이 곧 계약 |

### 결정: **토픽 1개 = 이벤트 1개**, 이름은 `MSG-<EVENT-NAME>`

| 발행 컨텍스트 | 이벤트 | 토픽 |
|---------------|--------|------|
| **Order** | `OrderCreated` | `MSG-ORDER-CREATED` |
| | `OrderConfirmed` | `MSG-ORDER-CONFIRMED` |
| | `OrderCancellationRequested` | `MSG-ORDER-CANCELLATION-REQUESTED` |
| | `OrderCancelled` | `MSG-ORDER-CANCELLED` |
| **Payment** | `PaymentCompleted` | `MSG-PAYMENT-COMPLETED` |
| | `PaymentFailed` | `MSG-PAYMENT-FAILED` |
| | `PaymentRefunded` | `MSG-PAYMENT-REFUNDED` |
| **Inventory** | `StockDeducted` | `MSG-STOCK-DEDUCTED` |
| | `StockShortage` | `MSG-STOCK-SHORTAGE` |
| | `StockRestored` | `MSG-STOCK-RESTORED` |

**네이밍 규약**: `MSG-` 접두 + 이벤트명을 대문자 하이픈(SCREAMING-KEBAB)으로. 새 이벤트 추가 = 새 토픽 추가, §7의 "깨지는 변경은 새 `eventType`" 규약도 새 토픽 신설로 이어진다.

**근거**
- **구독이 곧 계약** — 구독자는 자기가 처리할 이벤트의 토픽만 구독한다(`@KafkaListener(topics = "MSG-ORDER-CREATED")`). 관심 없는 메시지를 받아 버리는 낭비와, 그 필터링 로직 자체가 사라진다.
- **역직렬화가 단정적** — 토픽마다 payload 타입이 하나뿐이라 `eventType`으로 분기해 타입을 고르는 디스패치가 필요 없다. 리스너 시그니처에 타입을 바로 박을 수 있다.
- **운영 단위 분리** — 컨슈머 그룹·랙·파티션 수·리텐션을 이벤트 단위로 조절할 수 있다. `OrderCreated`만 지연되는 상황을 다른 이벤트와 섞이지 않은 지표로 본다.
- **장애 격리** — 한 이벤트의 처리 실패·재처리가 같은 토픽에 실린 다른 이벤트의 소비를 막지 않는다(컨텍스트별 단일 토픽에서는 head-of-line blocking이 생긴다).

**트레이드오프**
- 토픽이 3개 → 10개로 늘어 생성·설정 대상이 많아진다(D4 ✗). 학습 프로젝트 규모에서는 감내 가능하며, 토픽 생성은 구성으로 일괄 관리한다(`KAFKA_AUTO_CREATE_TOPICS_ENABLE: false`이므로 명시 생성 필요).
- 한 주문의 이벤트가 10개 토픽에 흩어져 **토픽 내 순서 보장의 적용 범위가 좁아진다** → §6에서 다룬다.
- envelope `eventType`은 토픽과 정보가 중복된다. 그래도 유지하는 이유: Outbox 컬럼(`event_type`)과 1:1이고(§4, D2), 토픽에서 분리된 뒤(로그·Inbox 적재·DLQ)에도 메시지가 자기 정체를 스스로 말할 수 있어야 한다.

---

## 6. 결정 D — 파티션 키 / 순서 보장

### 결정: 파티션 키 = `orderId` (policy PI-4 확정 재확인)

- 같은 주문의 이벤트는 **한 토픽 내 동일 파티션**에 적재 → 토픽 내 순서 보장.
- **학습 포인트(명시)**: 순서 보장은 *토픽-파티션 내*에서만 성립. §5에서 토픽을 이벤트별로 쪼갰으므로 한 주문의 이벤트는 최대 10개 토픽에 흩어지고, **토픽 간 전역 순서는 보장되지 않는다.** 이는 문제가 아니다 — 인과 순서는 **사가 흐름 자체가 강제**한다(Inventory는 `PaymentCompleted`를 받아야만 차감). 코레오그래피가 토픽 간 전역 순서에 *의존하면 안 되는* 이유의 사례.
- 이벤트별 토픽에서 `orderId` 키가 실제로 지키는 것은 **같은 이벤트 타입의 재발행 순서**다(예: 재시도로 두 번 발행된 `MSG-ORDER-CREATED`). 그 이상의 순서는 위 문단대로 사가와 Inbox 멱등(PI-5)이 책임진다.

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

- 토픽 = 이벤트 1:1이라 구독자는 관심 이벤트만 구독하고, 리스너에서 필터·디스패치 없이 payload 타입을 확정한다(§5).

**부정 / 주의**
- 토픽 수가 이벤트 수만큼 늘어난다(현재 10개). 자동 생성이 꺼져 있으므로 **이벤트를 추가할 때 토픽 생성도 함께** 해야 하고, 빠뜨리면 발행 실패로 드러난다.
- 가산 규약은 *규율*이라 컴파일러가 강제 못 함 — 호환성 위반은 리뷰로 막는다.
- payload JSON은 스키마 강제가 약함(레지스트리 없음) — 학습 범위의 의도적 단순화.

**다른 문서 영향**
- **policy.md §1**: Order 이벤트에 `OrderCancellationRequested` 추가, 보상 흐름에 Order-개시 경로 반영.
- **ADR-0003**: `OrderTimedOut` → `OrderCancellationRequested(reason=TIMEOUT)`로 정합.

---

## 9. 미해결 (이 ADR 범위 밖)

- 클라이언트 멱등키 저장·인바운드 응답 모델 → **ADR-0006**(별도 세션). 멱등키는 이 계약에 포함하지 않는다(경계).
- Inbox 키 `event_id` 단독 vs `(event_id, handler)` 복합 → ADR-0004 §7, 구독 토폴로지 확정 시. (한 컨텍스트가 같은 토픽을 여러 핸들러로 처리하면 복합키 필요.)
- 토픽 파티션 수·리텐션·컨슈머 그룹 구성 — 운영 튜닝(코드 착수 시). 이벤트별 토픽이므로 이벤트마다 값이 달라질 수 있다.
- 토픽 생성 방식(구성 파일 일괄 선언 vs `NewTopic` 빈) — §5의 10개 토픽을 어디서 관리할지.
- `schemaVersion`/레지스트리 도입 — 호환성 관리가 필요해지는 후속 학습.
