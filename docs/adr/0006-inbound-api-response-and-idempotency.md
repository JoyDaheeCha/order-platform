# ADR-0006: 인바운드 주문 API — 응답 모델 & 멱등키 저장

- **상태(Status)**: Accepted (2026-06-27)
- **관련 정책**: [policy.md](../policy.md) §3(PI-1·2·3 생산자 측 멱등성), §4(PS 상태) / [product-spec.md](../product-spec.md) §4(해피패스), PI-1
- **선행 결정**: [ADR-0001](./0001-saga-orchestration-vs-choreography.md) 코레오그래피(비동기), [ADR-0004](./0004-schema-separation-outbox-readmodel.md) `order_saga_progress` read model, [ADR-0003](./0003-order-deadline-checker.md) 폴링 스위퍼
- **후속 의존**: 핵심 테이블 스키마(`orders` 등), 인바운드 REST API 스펙

---

## 1. 맥락 (Context)

구매자의 외부 커맨드는 `PlaceOrder`·`CancelOrder` 둘뿐이다(policy §1). 그런데 **동기 HTTP 요청이 비동기 코레오그래피 Saga를 어떻게 응답하느냐**가 어느 문서에도 결정돼 있지 않다. `product-spec §4`가 이를 드러낸다:

- step 2: 주문 `PENDING` 생성 → `OrderPlaced` 발행 (여기서 HTTP 요청은 종료돼야 정상)
- step 6: "구매자에게 **주문 확정**(CONFIRMED) 응답" ← 결제·재고가 비동기로 끝난 *뒤*의 상태

이 둘은 모순이다. 결제·재고는 Kafka로 비동기 진행(최대 사가 타임아웃 30s, ADR-0003)되므로, HTTP 요청이 `CONFIRMED`까지 동기로 기다릴 수 없다.

또한 `policy §3`은 멱등키의 **규칙**(PI-1 UUID 동반, PI-2 전역 유니크·TTL 24h·기존 결과 반환, PI-3 다른 페이로드면 409)만 정했고, **이 키를 어디에 어떻게 저장/조회하는지**는 미정이다. ADR-0004의 Inbox는 *소비자 측*(PI-5, `eventId`)이라 *생산자 측 요청 멱등키*(PI-1~3)와는 별개다.

이 ADR은 두 가지를 확정한다: **(A) 인바운드 응답 모델**, **(B) 멱등키 저장 메커니즘**.

---

## 2. 결정 기준 (Decision Drivers)

| # | 기준 | 가중치 |
|---|------|--------|
| D1 | **EDA 정합성** — 비동기 Saga 원칙과 충돌하지 않는가 | 높음 |
| D2 | **기존 설계 재활용** — read model(ADR-0004)·스위퍼(ADR-0003)를 그대로 쓰는가 | 중간 |
| D3 | **정책 충족도** — PI-1·2·3을 정확히 표현하는가 | 높음 |
| D4 | **단순성** — 추가 인프라·복잡도가 적은가 | 중간 |

---

## 3. 결정 A — 인바운드 응답 모델

### 고려한 옵션
| 옵션 | 방식 | EDA(D1) | 비고 |
|------|------|---------|------|
| **A-1** ✅ | `202 Accepted` + `{orderId, status:PENDING}` 즉시 반환 → 클라이언트가 **`GET /orders/{id}` 폴링**으로 최종 상태 확인 | ◎ 순수 비동기 | read model 조회(D2 ◎) |
| A-2 | `202` + **SSE/WebSocket 푸시**로 확정 통지 | ◎ | UX↑이나 인프라·복잡도↑ → 확장 과제 |
| A-3 | **sync-over-async** — HTTP 요청을 `CONFIRMED`/`CANCELLED`까지 블록 | ✗ | HTTP 스레드를 사가 완료(최대 30s)까지 점유 → 스레드 고갈·EDA 위배 |

### 결정: **A-1 (202 + 폴링)**
- 코레오그래피의 비동기성과 정확히 맞는다(D1). 요청은 `OrderPlaced` 발행 직후 즉시 반환한다.
- **조회 대상 = ADR-0004의 `order_saga_progress` read model.** 설계가 이미 깔려 있어 그대로 재활용(D2). "주문이 어디까지 진행됐나"(ADR-0001 §5)를 클라이언트가 폴링으로 본다.
- **A-3 기각**: HTTP 스레드를 사가 완료까지 잡으면 동기 결합이 부활하고, 타임아웃(30s) 동안 스레드를 점유해 고갈된다. EDA의 핵심을 정면으로 위배(D1).
- A-2(SSE)는 폴링 학습 후의 **확장 과제**로 둔다.

> product-spec §4 step 6의 "확정 응답"은 **"접수(`202 PENDING`) 응답"으로 정정**한다(확정은 폴링으로 확인). → §5 후속에 문서 수정 명시.

---

## 4. 결정 B — 멱등키 저장 메커니즘

### 고려한 옵션
**B-1 — 전용 `idempotency_keys` 테이블 (order_schema)** ✅
```
idempotency_keys
  idem_key      CHAR(36) PK    -- 클라이언트 UUID (PI-1)
  order_id      VARCHAR        -- 이 키로 생성된 주문 (PI-2: 재요청 시 이 주문 결과 반환)
  request_hash  CHAR(64)       -- 요청 페이로드 해시 (PI-3: 같은 키·다른 페이로드 → 409)
  created_at    DATETIME
  expires_at    DATETIME       -- created_at + 24h (PI-2 TTL); 스위퍼가 만료행 정리
```
- 장점: PI-1·2·3을 **그대로 표현**(D3 ◎). 만료는 `expires_at` + **폴링 스위퍼(ADR-0003 패턴 재활용)**로 정리(D2). order 컨텍스트의 관심사라 order_schema에 자연 귀속.
- 단점: 테이블 1개 추가.

**B-2 — `orders` 테이블에 `idempotency_key` UNIQUE 컬럼**
- 단점: 멱등성 관심사가 주문 본체에 섞임. PI-3(페이로드 해시 비교)·TTL 후 재사용을 표현하기 어색(orders에 hash·expires 컬럼을 덧대야 함). (D3 △)

**B-3 — Redis (`SETNX` + TTL 24h)**
- 장점: TTL이 자연스러움(D4 부분 ◎).
- 단점: PI-2의 "기존 주문 결과 반환"을 위해 결국 `key→order_id` 매핑을 영속해야 함 → Redis 영속성 의존·이중 소스. 단일 인스턴스·학습 규모엔 오버(D4 ✗).

### 결정: **B-1 (전용 테이블)**
PI-1·2·3을 가장 명확히 표현하고, TTL 정리에 ADR-0003 스위퍼를, 저장에 ADR-0004 컨텍스트 스키마를 재활용한다. Redis(B-3)는 재고(ADR-0002 C)와 함께 **확장 시 재검토**.

세 가지 정련(구현 시 반드시 지킬 것):

1. **원자성 — order insert와 같은 트랜잭션**: `idempotency_keys` 행과 `orders` 행을 *한 로컬 트랜잭션*에 쓴다(둘 다 order_schema, 같은 DataSource). 분리되면 크래시 시 "주문 생성·키 미기록 → 재시도 중복" 또는 "키 기록·주문 없음" 이 발생 — Outbox(PI-6)가 푸는 dual-write 문제와 동일. **멱등성도 결국 원자성 문제**라는 게 핵심 학습 포인트.
2. **동시성 — PK 직렬화(insert-or-catch)**: `idem_key`를 PK로 두고, "조회 후 삽입"이 아니라 **INSERT를 시도해 중복키 위반을 잡는다**. 동시 중복 요청의 race(오버셀 PV-4와 같은 부류)를 DB 제약이 직렬화한다.
3. **저장 최소 — order_id만**: A-1 덕에 응답이 `202 {orderId, PENDING}`로 단순하므로, 전체 응답 직렬화 대신 **order_id만 저장**해도 PI-2를 충족한다. 최종 상태는 read model 폴링으로 본다.

**TTL 정리는 학습 대상으로 명시한다**: correctness가 아니라 *운영 위생*(핫 패스 인덱스 성장·버퍼풀 오염·저장 비용)을 위해 만료 행을 정리한다. 1단계는 **폴링 스위퍼(ADR-0003 패턴)** 로 `expires_at < now()` 삭제. 대규모에선 대량 DELETE 자체가 비싸 **시간 파티셔닝 + 파티션 DROP**으로 가는 것까지 비교 학습한다(확장 메모).

---

## 5. 결과 / 영향 (Consequences)

**긍정**
- 비동기 Saga ↔ 동기 HTTP 경계가 명확해진다: **접수는 즉시, 확정은 폴링**.
- read model(ADR-0004)·스위퍼(ADR-0003)가 **새 용도로 재활용** → 설계 일관성↑, 추가 인프라 0.
- 멱등키 저장이 PI-1~3을 1:1로 충족.

**부정 / 주의**
- **폴링 비용**: 클라이언트가 `GET /orders/{id}`를 반복 → 합리적 간격·종료조건(terminal 상태) 가이드 필요. (학습 범위에선 단순 폴링)
- `idempotency_keys`·`order_saga_progress` 두 부수 테이블의 스위퍼 정리 주기 관리.
- **문서 수정 필요**: product-spec §4 step 6 "확정 응답" → "접수(202) 응답 + 폴링으로 확정 확인".

---

## 6. 미해결 (이 ADR 범위 밖)

- 폴링 간격·최대 폴링 시간·`Retry-After` 헤더 사용 여부 — REST API 스펙 작성 시.
- A-2(SSE/WebSocket 푸시)는 확장 과제.
- `request_hash` 산출 대상(정규화 범위: 라인 정렬·필드 화이트리스트) — API 스펙 시 확정.
- `idempotency_keys` TTL 스위퍼를 ADR-0003 스위퍼와 **공유 잡**으로 둘지 별도 둘지 — 구현 시.
