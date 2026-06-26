# ADR-0003: Order 데드라인 체커 · 재시도 / DLQ 구성

- **상태(Status)**: Accepted (2026-06-27)
- **관련 정책**: [policy.md](../policy.md) §8(PB), §9(PT-1~4) / [architecture.md](../architecture.md) §5(데드라인 체커 위치)
- **선행 결정**: [ADR-0001](./0001-saga-orchestration-vs-choreography.md) 코레오그래피(중앙 오케스트레이터·중앙 Saga 상태 없음 → §5 주의), [ADR-0004](./0004-schema-separation-outbox-readmodel.md) `order_saga_progress` read model · Inbox 멱등성
- **후속 의존**: 없음(이 ADR로 PT 정책의 구현 결정이 닫힌다)

---

## 1. 맥락 (Context)

코레오그래피에는 "결제 이벤트가 안 온다"를 지켜보는 **중앙 감시자가 없다**(ADR-0001 §5). 그러나 정책 PT-1은 *"Saga 각 단계는 타임아웃을 가지며, 한계를 넘으면 실패로 간주하고 보상으로 전환한다"*를 요구한다. 누군가는 "기한 내 종료 이벤트가 없음"을 감지해 취소(보상)를 트리거해야 한다.

사가의 개시자이자 진행도를 자체 추적하는(PC-4, ADR-0004 `order_saga_progress`) **Order 컨텍스트가 데드라인 주체**가 된다. 이 ADR은 두 가지를 확정한다:

1. **데드라인 체커**: 입도(전체 vs 단계별) + 구현 메커니즘(폴링 스위퍼 vs 지연 메시지).
2. **재시도 / DLQ 구성**: PT-2~4를 Spring Kafka에서 어떻게 구현할지.

> 이 프로젝트의 목적은 제품 출시가 아니라 **분산 패턴의 깊은 이해**(todo.md)다. 결정 기준에서 "학습 가치"의 가중치가 가장 높다(ADR-0001과 동일).

---

## 2. 결정 기준 (Decision Drivers)

| # | 기준 | 가중치 |
|---|------|--------|
| D1 | **학습 가치** — 타임아웃 감지·멱등 전이·재시도/DLQ를 *명시적으로* 학습 가능한가 | 높음 |
| D2 | **크래시 안전성** — 프로세스가 죽어도 밀린 타임아웃이 유실되지 않는가 | 높음 |
| D3 | **결합도** — 코레오그래피의 도메인 격리(ADR-0001)를 깨지 않는가 | 중간 |
| D4 | **구현/운영 복잡도** — 학습 범위에서 감당 가능하고 추가 인프라를 강요하지 않는가 | 중간 |

---

## 3. 결정 A — 타임아웃 입도 (Granularity)

### 고려한 옵션

| 옵션 | 방식 | 트레이드오프 |
|------|------|--------------|
| **A-1** ✅ | **사가 전체 타임아웃** — "PENDING으로 들어온 주문이 N초 안에 종료상태(CONFIRMED/CANCELLED)에 도달" | Order가 알 것은 "종료했나/아닌가" 하나. 어느 단계가 느린지는 구분 못 함 |
| A-2 | **단계별 타임아웃** — 결제 단계 X초, 재고 단계 Y초 각각 데드라인 | 정밀하나, Order가 "지금 어느 단계 대기 중"을 상태로 모델링해야 함 |

### 결정: **A-1 (사가 전체 타임아웃) 채택**

- **A-2 기각 근거 — 오케스트레이터 재발명**: 단계별 타임아웃을 하려면 Order가 `AWAITING_PAYMENT`/`AWAITING_STOCK` 같은 in-flight 단계 상태 + 단계별 마감시각 + "이벤트 수신 시 전이 + 다음 시계 재설정" 로직을 떠안아야 한다. 이는 **상태 기계 = 오케스트레이터의 본체**다. 코레오그래피("Order는 자기 일만")의 도메인 격리를 깨고(D3), ADR-0001이 *후속 학습 과제로 분리한* "오케스트레이션 재구현"을 이 ADR로 앞당겨 중복시킨다.
- **실무 정합**: 성숙한 시스템도 단계별 타임아웃이 필요해지는 순간 손으로 짜지 않고 **워크플로 엔진(Temporal·Step Functions·Camunda·Axon DeadlineManager)** 의 durable timer로 간다. 즉 단계별 = "오케스트레이션/엔진 전제". 단계 3개·선형인 우리 규모의 순수 코레오그래피에서는 **전체 타임아웃(상태 테이블 + 주기 스캔)이 실무에서도 정석인 build-it-yourself 패턴**이다.
- 입도와 보상은 별개다: "어디까지 진행됐나"(PB-1 역순 보상)는 `order_saga_progress`의 *수동 진행도 기록*으로 판단하지, 데드라인 시계로 판단하지 않는다.

> 단계별 타임아웃은 버리는 게 아니라 **의도적으로 미룬다**(§7). "단계별이 필요한 순간 = 워크플로 엔진으로 가는 순간"을 직접 겪는 것이 ADR-0001 학습 아크의 다음 단계다.

---

## 4. 결정 B — 데드라인 체커 메커니즘

### 고려한 옵션

| 옵션 | 방식 | D2 크래시 | D4 복잡도 |
|------|------|-----------|-----------|
| **B-1** ✅ | **폴링 스위퍼** — 상태 테이블의 만료 행을 주기 스캔 | ◎ 상태가 DB에 있어 재시작이 밀린 만료를 줍는다 | ◎ 추가 인프라 0 |
| B-2 | **지연 메시지** — "T+데드라인에 점검" 메시지를 예약 배달 | 변형마다 다름(아래) | ✗ Kafka 네이티브 부재 |

### 결정: **B-1 (폴링 스위퍼) 채택**

- **B-2 기각 근거 — Kafka엔 네이티브 지연 메시지가 없다**(SQS `DelaySeconds`·RabbitMQ delayed-exchange와 달리). durable하게 만들려는 모든 변형이 결국 폴러로 회귀한다:
  - Redis ZSET / DB 지연 테이블 + 폴러 → **사실상 스위퍼**.
  - per-delay 토픽 휠·tombstone replay → 과한 복잡도(D4 ✗).
  - `TaskScheduler.schedule(instant)` 인메모리 타이머 → **재시작 시 소실**(D2 ✗), 결국 DB에서 재수화 필요 → 또 상태 테이블.
  - 즉 "이벤트답게 push" 직관이 Kafka에선 비싸고, durable 변형은 상태 테이블 + 폴러로 수렴한다. 이는 ADR-0001 테마("횡단 관심사를 다루다 보면 오케스트레이터를 재발명")의 인프라판이다.
- **A-1을 고른 순간 B-1로 수렴**한다: 전체 타임아웃은 `order_saga_progress`에 마감시각 하나만 있으면 되고, 그 테이블은 ADR-0004가 이미 둔다 → 지연 메시지를 끌어올 이유가 없다.

### 데이터 — `order_saga_progress`에 `deadline_at` 추가 (ADR-0004 테이블 재사용)

새 테이블을 만들지 않고 ADR-0004 §5 C-3의 read model을 확장한다.

```
order_saga_progress   -- order_schema 소유 (ADR-0004)
  order_id          VARCHAR PK
  status            VARCHAR   -- PENDING/PAID/CONFIRMED/CANCELLED
  payment_completed DATETIME NULL
  stock_deducted    DATETIME NULL
  deadline_at       DATETIME      -- ★ 추가: OrderPlaced 시 now()+N (전체 사가 마감)
  version           BIGINT        -- ★ 추가: 낙관적 락 (정상 전이 vs 타임아웃 전이 경합 정리)
  updated_at        DATETIME
```

- 인덱스 `(status, deadline_at)` — 만료 스캔이 PENDING+만료분만 집도록.

### 스위퍼 (`order.infrastructure`) → 유스케이스 (`order.application`)

```java
// infrastructure: 트리거만
@Scheduled(fixedDelayString = "${order.saga.sweep-interval:5s}")
void sweep() {
    for (var id : repo.findExpired(PENDING, now()))   // status=PENDING AND deadline_at < now()
        timeoutUseCase.handle(id);                    // 인바운드 포트
}

// application: 판정·전이 (헥사고날 A-6 — 스케줄러는 어댑터일 뿐)
@Transactional
void handle(orderId) {
    var p = repo.find(orderId);
    if (p.status != PENDING) return;        // ★ 멱등 가드 — 이미 종료/이전 스캔 처리분 no-op
    p.markTimedOut();                        // PENDING → CANCELLING
    repo.save(p);                            // 낙관적 락(version)으로 경합 시 한쪽만 성공
    outbox.append(new OrderCancellationRequested(orderId, TIMEOUT)); // 보상-개시 이벤트(ADR-0007)
}
```

### 핵심 학습 포인트

- **경합(race)**: 스위퍼가 "만료"로 판단한 직후 `PaymentCompleted`가 도착할 수 있다. 정상 진행 핸들러와 타임아웃 핸들러가 **같은 행을 두고 경쟁** → 낙관적 락(`version`)/행 잠금으로 **둘 중 하나만 이긴다**를 보장하고, 진 쪽은 멱등 가드(`status != PENDING`)에서 no-op. 이는 PC-4(사용자 취소 경합)와 동형 문제 — "중앙 상태가 없어 Order가 자체 동시성을 정리해야 한다"는 코레오그래피의 대가.
- **멱등성(PT-3)**: 스위퍼가 주기적으로 돌아도, 전이 후 `status`가 PENDING을 벗어나 다음 스캔에서 자동 제외 + 가드로 이중 안전.
- **크래시 안전성(D2)**: 상태가 DB에 있어, 스캔 직전 죽어도 재시작 후 다음 스캔이 밀린 만료를 그대로 줍는다(인메모리 타이머가 못 가지는 속성).

### 데드라인 / 주기 값

| 항목 | 설정 키 | 기본값 | 비고 |
|------|---------|--------|------|
| 전체 사가 데드라인 N | `order.saga.deadline` | 30s | 정상 mock 흐름 대비 충분히 김 |
| 스위퍼 주기 | `order.saga.sweep-interval` | 5s | 검출 지연 = 최대 1주기(정밀도 = 주기) |

- 값을 코드에 박지 않고 `@ConfigurationProperties`로 뺀다. **테스트는 짧은 값(`deadline=2s, interval=500ms`)으로 오버라이드**해, "결제 이벤트를 일부러 안 보내고 → 곧 `OrderCancellationRequested(TIMEOUT)`·`CANCELLED` 확인" 경로를 수 초 안에 재현한다.

### 타임아웃 → 보상 연결

`OrderCancellationRequested(reason=TIMEOUT)` 발행 후 보상(PB)으로 흐른다(보상-개시 이벤트, ADR-0007). 진행분만 역순 보상(PB-1), 각 보상은 멱등(PB-2):

```
OrderCancellationRequested(TIMEOUT) ─▶ [Payment] 결제됐으면 RefundPayment
              ─▶ [Inventory] 차감됐으면 RestoreStock
              ─▶ [Order] status = CANCELLED
```

---

## 5. 결정 C — 재시도 / DLQ (PT-2~4)

### C-1. 두 실패를 절대 섞지 않는다 (가장 중요)

| 부류 | 예 | 처리 | 근거 |
|------|----|------|------|
| **비즈니스 실패** | 재고부족(PV-3)·결제거절(PP-3) | 재시도 ❌ → **즉시 실패 이벤트 발행 → 보상** | PB-4. 정상 사가 분기, **DLQ 아님** |
| **일시적 인프라 오류** | DB 순단·네트워크 타임아웃·역직렬화 실패 | 지수 백오프 재시도 → 한계 초과 시 DLQ | PT-2 |

> 구현 규약: 비즈니스 실패는 **예외로 던지지 않고** 핸들러가 정상 리턴하며 실패 이벤트를 발행한다. 인프라 오류만 예외로 터뜨려 에러 핸들러(재시도/DLQ)에 태운다. 재고부족을 예외→재시도로 흘리면 "정상 결과가 장애처럼 DLQ를 오염"시킨다.

### C-2. Spring Kafka 구성

`DefaultErrorHandler` + `ExponentialBackOff` + `DeadLetterPublishingRecoverer`.

| 항목 | 설정 키 | 기본값 | 비고 |
|------|---------|--------|------|
| 초기 간격 | `kafka.retry.initial` | 1s | |
| 배수 | `kafka.retry.multiplier` | 2.0 | 1s → 2s → 4s |
| 최대 재시도 | `kafka.retry.max-attempts` | 4 | 초과 시 DLT |
| DLT 토픽 | (관례) | `<topic>.DLT` | |

- 비즈니스 예외는 `addNotRetryableExceptions(BusinessException.class)`로 재시도에서 제외(혹시 던져져도 바로 분기/DLT 처리).

### C-3. 재시도 전제 = 멱등성 (PT-3 → PI-5)

재시도는 같은 메시지의 재처리다. 멱등하지 않으면 부작용이 중복된다. 따라서 **모든 재시도 대상 컨슈머는 ADR-0004의 Inbox 중복제거(PI-5) 뒤에 위치**한다.

### C-4. DLQ는 유실이 아니다 (PT-4)

DLT로 간 메시지는 보존 + **로그/경보**로 인지하고, **수동 재처리 경로**(DLT → 원본 토픽 재발행 트리거)를 둔다. 학습용 최소 구현 — 자동 재처리 파이프라인은 범위 밖.

---

## 6. 결과 / 영향 (Consequences)

**긍정**
- 타임아웃 감지 주체(Order)·멱등 전이·경합 정리를 *명시적으로* 학습(D1). 상태 기반이라 크래시에 강함(D2).
- 추가 인프라 0 — ADR-0004 테이블에 컬럼 2개(`deadline_at`·`version`)만 추가, 스프링 스케줄러로 충분(D4).
- 비즈니스 실패 ↔ 인프라 오류 분리로 보상 경로와 재시도/DLQ 경로가 깨끗이 갈린다.

**부정 / 주의 (= 의식적 설계 포인트)**
- **검출 지연 = 스위퍼 주기**: 정밀도가 주기에 묶인다(주문 타임아웃엔 충분, 정밀 SLA엔 부적합).
- **단계 구분 불가**: 전체 타임아웃은 "어느 단계가 느린지" 자체로는 모른다 → 상관관계 키(`orderId`)로 로그·read model 사후 추론(§7).
- **다중 인스턴스**: 스위퍼가 여러 인스턴스서 동시 실행되면 잡 중복 → `SKIP LOCKED`/ShedLock 필요. **단일 배포라 현재는 무관**, 서비스 분리 시 도입.
- **경합 처리 필수**: 낙관적 락/가드를 빠뜨리면 정상 확정과 타임아웃 취소가 동시에 적용될 수 있다(PC-4와 동형).

---

## 7. 미해결 (이 ADR 범위 밖)

- **단계별 타임아웃·하트비트** → 동일 도메인을 **오케스트레이션/워크플로 엔진으로 재구현**할 때 다룬다(ADR-0001 후속 학습 과제). 그때 durable timer(Temporal·Axon DeadlineManager 등)로 단계별 데드라인을 *공짜로* 얻는 트레이드오프를 비교.
- 스위퍼 다중 인스턴스 동시성(`SKIP LOCKED`/ShedLock) — 컨텍스트를 별도 서비스로 분리할 때.
- DLT 자동 재처리 파이프라인 — 학습 최소 구현(수동) 이후 확장 과제.
- 재시도/백오프 정확한 튜닝 값 — 실제 부하·실패 패턴 관측 후(현재는 학습용 기본값).
