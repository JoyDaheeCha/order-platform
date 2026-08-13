# ADR-0004: 데이터 영속화 — 스키마 분리 · Outbox/Inbox · 주문 상태 Read Model

- **상태**: Accepted (2026-06-27)
---

## 1. 맥락
모듈러 모노리스는 **단일 MySQL 인스턴스**를 쓰지만, 컨텍스트 경계는 마이크로서비스 수준으로 강제해야 한다.  


## 2. 요구사항
- 주문, 결제, 재고의 데이터 경계를 물리적으로 제한하라
- saga에서 쓸 Outbox/Inbox 구조를 정하라
- 주문 상태 read model 구조를 정하라.

### 참고) 세 결정이 서로 미치는 영향
- 스키마분리
  - 컨텍스트 간 JOIN·FK·단일 트랜잭션이 불가능 → **read model**이 필요
  - "상태 변경 + 이벤트 발행"을 한 트랜잭션으로 못 묶음(dual-write) → **Outbox**가 필요
---

## 3. 결정 기준
- 컨텍스트 간 데이터 결합(JOIN/FK)을 구조적으로 차단
- MSA 분리 용이성
---

## 4. 결정: 스키마 구성
### 4.1 선택된 내용
컨텍스트별 스키마 분리, 단일 MySQL 인스턴스

### 4.2 기각된 옵션
- 단일 스키마, 소유 테이블만 접근하도록 컨벤션 지정 -> 사람 실수에 의해 경계 무너지기 쉬움
- DB 인스턴스까지 분리 -> 학습 프로젝트에는 오버스펙 

### 4.3 구조
```
단일 MySQL 인스턴스
├── order_schema       (orders, order_outbox, order_inbox, order_saga_progress)
├── payment_schema     (payments, payment_outbox, payment_inbox)
└── inventory_schema   (stocks, inventory_outbox, inventory_inbox)
```

---

## 5. 결정: — 영속화 구성 (DataSource / 트랜잭션 경계)
**컨텍스트별 DataSource / EntityManagerFactory / TransactionManager** ✅(권장)
- 장점: 다른 스키마 테이블이 **EntityManager에 아예 안 보임 → cross-schema 접근 물리적 불가**. 추후 서비스 분리 시 DataSource만 떼면 됨.
- 단점: 컨텍스트당 영속화 `@Configuration` 보일러플레이트. 트랜잭션 매니저가 컨텍스트별로 나뉜다

---

## 6. 결정 — Outbox / Inbox / Read Model

### 6.1 Outbox 
- 상태 변경과 통합 이벤트 발행의 원자성을 위해, 도메인 변경과 **같은 로컬 트랜잭션**에서 `outbox`에 이벤트를 적재.
- 별도 릴레이 혹은 트랜잭션 설정으로 발행

```
outbox
  id            BIGINT PK AUTO
  event_id      CHAR(36)  -- UUID, 통합 이벤트의 멱등키 (PI-4) → 소비자 Inbox 키
  aggregate_id  VARCHAR   -- orderId (상관관계 키, PI-4)
  event_type    VARCHAR   -- 'OrderCreated' 등 (shared 계약명)
  topic         VARCHAR   -- 발행 대상 토픽
  payload       JSON      -- 통합 이벤트 직렬화 본문 (C-4: 도메인 이벤트→통합 이벤트 변환 결과)
  occurred_at   DATETIME
  published_at  DATETIME NULL  -- NULL=미발행 (릴레이 폴링 대상)
```

### 6.2 Inbox 

- 소비자는 처리 전 `inbox`에 `event_id` 존재를 확인, 최초 1회만 처리(at-least-once → effectively-once)
- 7일 경과후 제거

```
inbox
  event_id     CHAR(36) PK   -- 중복 소비 차단 키 (PI-4의 eventId)
  handler      VARCHAR       -- 동일 이벤트를 여러 핸들러가 구독할 때 (event_id, handler) 복합키 고려
  received_at  DATETIME
  processed_at DATETIME NULL
  -- 7일 경과 행은 배치/이벤트로 정리
```

### 6.3 주문 상태 Read Model

- order가 "어디까지 진행됐나"를 JOIN으로 못 보므로, **order_schema 안에** 수신 이벤트로 진행도를 누적하는 read model을 둔다.
- 흐름이 토픽에 흩어지는 코레오그래피의 단점을 보완
```
order_saga_progress   -- order_schema 소유, order의 이벤트 소비자가 갱신
  order_id          VARCHAR PK
  status            VARCHAR   -- PENDING/PAID/CONFIRMED/CANCELLED (PS 상태와 동기)
  payment_completed DATETIME NULL  -- PaymentCompleted 수신 시각
  stock_deducted    DATETIME NULL  -- StockDeducted 수신 시각
  updated_at        DATETIME
```
---