# ADR-0004: 데이터 영속화 — 스키마 분리 · Outbox/Inbox · 주문 상태 Read Model

- **상태(Status)**: Accepted (2026-06-27)
- **관련 정책**: [policy.md](../policy.md) §3(PI-4·5·6), §4(PS), §7(PC-3·4) / [architecture.md](../architecture.md) C-2·C-4·§5
- **선행 결정**: [ADR-0001](./0001-saga-orchestration-vs-choreography.md) 코레오그래피 (중앙 Saga 상태 없음 → §5 주의)
- **후속 의존**: ADR-0002(재고 동시성), ADR-0003(타임아웃/DLQ)

---

## 1. 맥락 (Context)

모듈러 모노리스는 **단일 MySQL 인스턴스**(todo.md)를 쓰지만, 컨텍스트 경계는 마이크로서비스 수준으로 강제해야 한다(architecture.md §1). 이 ADR은 그 경계를 **데이터 레벨에서** 어떻게 그을지, 그리고 코레오그래피 Saga가 요구하는 두 인프라 패턴(Outbox/Inbox)과 한 보조 구조(주문 상태 read model)를 **어디에 어떤 테이블로** 둘지 확정한다.

세 결정은 서로 맞물린다:
- **스키마를 분리**하면 → 컨텍스트 간 JOIN·FK·단일 트랜잭션이 불가능해지고,
- 그 결과 "주문이 어디까지 진행됐나"를 JOIN으로 못 보므로 → **read model**이 필요하며,
- "상태 변경 + 이벤트 발행"을 한 트랜잭션으로 못 묶으므로(dual-write) → **Outbox**가 필요하다(PI-6).

> 선행 제약: C-2(공유 DB 테이블 금지), C-4(도메인 이벤트 ≠ 통합 이벤트), PI-4/5/6(멱등성·Outbox·Inbox).

---

## 2. 결정 기준 (Decision Drivers)

| # | 기준 | 가중치 |
|---|------|--------|
| D1 | **경계 강제력** — 컨텍스트 간 데이터 결합(JOIN/FK)을 구조적으로 차단하는가 (C-2) | 높음 |
| D2 | **학습 가치** — Outbox/Inbox/read model을 *명시적으로* 다루는가 (todo.md) | 높음 |
| D3 | **MSA 분리 용이성** — 후일 컨텍스트를 별도 서비스로 떼기 쉬운가 | 중간 |
| D4 | **구현/운영 복잡도** — 학습 범위에서 감당 가능한가 (Kafka/Saga 학습을 가리지 않는가) | 중간 |

---

## 3. 결정 A — 스키마 분리 수준

### 고려한 옵션

| 옵션 | 방식 | D1 경계 | D4 복잡도 |
|------|------|---------|-----------|
| A-0 | 단일 스키마, 테이블 공유 + JOIN 허용 | ✗ (가짜 경계) | 최소 |
| A-1 | 단일 스키마, 소유 테이블만 접근(규약) | △ (규약 의존) | 최소 |
| **A-2** ✅ | **컨텍스트별 스키마 분리, 단일 MySQL 인스턴스** | ◎ | 중간 |
| A-3 | DB 인스턴스까지 분리 | ◎ | 높음(오버스펙) |

### 결정: **A-2 채택**

컨텍스트마다 자기 스키마를 소유한다. 어떤 컨텍스트도 타 스키마를 read/write MUST NOT(C-2의 DB판).

```
단일 MySQL 인스턴스
├── order_schema       (orders, order_outbox, order_inbox, order_saga_progress)
├── payment_schema     (payments, payment_outbox, payment_inbox)
└── inventory_schema   (stocks, inventory_outbox, inventory_inbox)
```

- **A-0/A-1 기각**: 단일 배포의 편의를 위해 경계를 무르게 두면(JOIN 부활) A-4(컴파일 경계)와 모순. "DB로 몰래 연결된" 분산 모놀리스가 된다.
- **A-3 기각**: 단계가 3개뿐인 학습 범위에 인스턴스 분리는 오버스펙(D4). 단일 인스턴스로도 스키마 격리는 충분.

---

## 4. 결정 B — 영속화 구성 (DataSource / 트랜잭션 경계)

스키마를 분리했을 때 JPA를 어떻게 묶을지. 이 ADR에서 비용이 가장 큰 선택이었다(트레이드오프 검토 후 B-2 확정).

### 고려한 옵션

**옵션 B-1 — 단일 DataSource + 엔티티별 스키마 명시**
- 하나의 `DataSource`(기본 스키마 미지정), 각 엔티티가 `@Table(schema = "order_schema")`로 소속 선언. 단일 `TransactionManager`.
- 장점: 설정 최소, 트랜잭션 매니저 하나(D4 ◎).
- 단점: 같은 커넥션이라 **네이티브 쿼리로 cross-schema JOIN이 물리적으로 가능** → 경계가 "물리"가 아닌 "규약+리뷰" 강제(D1 △).

**옵션 B-2 — 컨텍스트별 DataSource / EntityManagerFactory / TransactionManager** ✅(권장)
- 컨텍스트마다 자기 스키마에 연결된 `DataSource` + `@EnableJpaRepositories(basePackages=".order.infrastructure", entityManagerFactoryRef=..., transactionManagerRef=...)`.
- 장점: 다른 스키마 테이블이 **EntityManager에 아예 안 보임 → cross-schema 접근 물리적 불가**(D1 ◎). 후일 서비스 분리 시 DataSource만 떼면 됨(D3 ◎).
- 단점: 컨텍스트당 영속화 `@Configuration` 보일러플레이트. 트랜잭션 매니저가 컨텍스트별로 나뉜다(어차피 컨텍스트 간 단일 트랜잭션은 금지이므로 정합).

### 결정: **B-2 채택**

architecture.md §1의 "마이크로서비스 수준 격리"와 D1·D3에 부합. cross-schema 결합을 *리뷰가 아니라 구조*가 막는다 — 각 컨텍스트의 `EntityManager`는 자기 스키마 테이블만 인식하므로 타 스키마 접근이 물리적으로 불가능하다.

- **B-1 기각**: 단일 커넥션이라 네이티브 쿼리로 cross-schema JOIN이 물리적으로 가능 → 경계가 규약·리뷰 의존(D1 △). 단일 트랜잭션 매니저의 단순함은 이점이나, 어차피 컨텍스트 간 단일 트랜잭션은 금지(Saga로 정합)라 그 이점이 이 도메인에선 무의미하다.
- **비용 인지**: 컨텍스트당 영속화 `@Configuration` 3세트(DataSource·EntityManagerFactory·TransactionManager)가 따른다. 이는 일회성 설정 비용으로 감수한다. 트랜잭션 매니저가 컨텍스트별로 나뉘는 것은 단점이 아니라 경계 정합(컨텍스트 간 단일 트랜잭션 금지와 일치).

---

## 5. 결정 C — Outbox / Inbox / Read Model

### C-1. Outbox (PI-6 — dual-write 해결)

상태 변경과 통합 이벤트 발행의 원자성을 위해, 도메인 변경과 **같은 로컬 트랜잭션**에서 `{context}_outbox`에 이벤트를 적재하고, 별도 릴레이가 비동기 발행한다.

**발행 방식 옵션**: (a) **폴링 릴레이**(스케줄러가 미발행 행을 주기 조회→Kafka 발행→`published_at` 기록) vs (b) CDC(Debezium).
→ **(a) 폴링 채택**: 흐름이 코드에 드러나 학습 명시성↑(D2), 인프라 단순(D4). CDC는 확장 과제.

```
{context}_outbox
  id            BIGINT PK AUTO
  event_id      CHAR(36)  -- UUID, 통합 이벤트의 멱등키 (PI-4) → 소비자 Inbox 키
  aggregate_id  VARCHAR   -- orderId (상관관계 키, PI-4)
  event_type    VARCHAR   -- 'OrderPlaced' 등 (shared 계약명)
  topic         VARCHAR   -- 발행 대상 토픽
  payload       JSON      -- 통합 이벤트 직렬화 본문 (C-4: 도메인 이벤트→통합 이벤트 변환 결과)
  occurred_at   DATETIME
  published_at  DATETIME NULL  -- NULL=미발행 (릴레이 폴링 대상)
```

### C-2. Inbox (PI-5 — 소비자 멱등성)

소비자는 처리 전 `{context}_inbox`에 `event_id` 존재를 확인, 최초 1회만 처리(at-least-once → effectively-once). 기록 TTL **7일**(PI-5).

```
{context}_inbox
  event_id     CHAR(36) PK   -- 중복 소비 차단 키 (PI-4의 eventId)
  handler      VARCHAR       -- 동일 이벤트를 여러 핸들러가 구독할 때 (event_id, handler) 복합키 고려
  received_at  DATETIME
  processed_at DATETIME NULL
  -- 7일 경과 행은 배치/이벤트로 정리
```

> 설계 주의: 한 이벤트를 여러 핸들러가 구독하면 `event_id` 단독 PK로는 두 번째 핸들러가 막힌다 → **(event_id, handler) 복합키**로 둘지 §6 후속에서 확정.

### C-3. 주문 상태 Read Model (ADR-0001 §5, PC-4)

코레오그래피는 중앙 Saga 상태가 없다(ADR-0001 §5). order가 "어디까지 진행됐나"를 JOIN으로 못 보므로(스키마 분리), **order_schema 안에** 수신 이벤트로 진행도를 누적하는 read model을 둔다.

```
order_saga_progress   -- order_schema 소유, order의 이벤트 소비자가 갱신
  order_id          VARCHAR PK
  status            VARCHAR   -- PENDING/PAID/CONFIRMED/CANCELLED (PS 상태와 동기)
  payment_completed DATETIME NULL  -- PaymentCompleted 수신 시각
  stock_deducted    DATETIME NULL  -- StockDeducted 수신 시각
  updated_at        DATETIME
```

- **용도 1 (PC-4 경합 직렬화)**: `PAID`↔`CONFIRMED` 전이 중 취소 요청이 오면 이 상태로 직렬화 판단.
- **용도 2 (가시성)**: 흐름이 토픽에 흩어지는 코레오그래피의 약점(ADR-0001 §5)을 보완하는 조회 모델.
- **경계 주의**: 이건 order가 **자기 이벤트 소비로 만든 사본**이지, payment/inventory 테이블을 보는 게 아니다(C-2 유지).

---

## 6. 결과 / 영향 (Consequences)

**긍정**
- 컨텍스트 간 JOIN·FK·분산 트랜잭션이 **구조적으로 차단** → 통신이 Kafka 이벤트로 강제(C-2)되고, 이것이 Saga·Outbox 학습을 *불가피하게* 만든다(D2).
- Outbox로 dual-write 해결, Inbox로 at-least-once 보정 — 분산 멱등성을 명시적으로 체득(PI-5·6).
- 컨텍스트별 스키마(+B-2시 DataSource)라 후일 서비스 분리가 평이(D3).

**부정 / 주의 (= 의식적 설계 포인트)**
- **데이터 중복**: order_saga_progress가 결제/재고 사실의 사본을 보유(정규화 위반처럼 보이나 컨텍스트 자율성을 위한 의도적 중복).
- **최종 일관성**: read model이 이벤트 도착까지 지연 — 조회 시점 stale 가능.
- **릴레이 운영**: 폴링 주기·실패 재발행·published_at 정합 관리 필요.
- **B-2 채택 시**: 멀티 DataSource 영속화 설정 보일러플레이트(§4 트레이드오프).

---

## 7. 미해결 (이 ADR 범위 밖)

- Inbox 키를 `event_id` 단독 vs `(event_id, handler)` 복합키로 둘지 — 구독 토폴로지 확정 후.
- Outbox 폴링 주기·배치 크기·published 행 보관/정리 정책 (운영 튜닝).
- 재고 차감의 동시성 기법(락 vs 원자 UPDATE vs Redis) → **ADR-0002**.
- Saga 단계 타임아웃 수치·데드라인 체커 구현·DLQ 구성 → **ADR-0003**.
- 토픽 네이밍 규칙·파티션 키(=orderId 상관관계) 표준 → shared 계약 확정 시.
