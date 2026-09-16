# ADR-0004: 데이터 영속화 — 스키마 분리 · Outbox/Inbox · 주문 상태 Read Model

- **상태**: Accepted (2026-06-27, 2026-09-16 구현 반영 갱신)
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
- 단일 스키마, 소유 테이블만 접근하도록 컨벤션 지정 -> 휴먼에러 막을수 없음
- DB 인스턴스까지 분리 -> 현재 규모에서는 오버스펙

### 4.3 구조
```
단일 MySQL 인스턴스 (localhost:13306)
├── order_schema       (orders, order_item, product, inbox, outbox)
├── payment_schema     (payment, inbox, outbox)
└── inventory_schema   (inventory, inventory_history, inbox, outbox)
```
> `inbox`·`outbox`는 스키마마다 동일한 이름으로 존재한다 (컨텍스트별 DataSource로 물리 격리되므로 이름 충돌이 없다).

---

## 5. 결정: — 영속화 구성 (DataSource / 트랜잭션 경계)
**컨텍스트별 DataSource / EntityManagerFactory / TransactionManager** ✅(권장)
- 장점: 다른 스키마 테이블이 **EntityManager에 아예 안 보임 → cross-schema 접근 물리적 불가**. 추후 서비스 분리 시 DataSource만 떼면 됨.
- 단점: 컨텍스트당 영속화 `@Configuration` 보일러플레이트. 트랜잭션 매니저가 컨텍스트별로 나뉜다

---

## 6. 결정 — Outbox / Inbox / Read Model

### 6.1 Outbox
- 상태 변경과 통합 이벤트 발행의 원자성을 위해, 도메인 변경과 **같은 로컬 트랜잭션**에서 `outbox`에 이벤트를 적재.
- 릴레이(폴링) 스케줄러가 `status = CREATED`인 행을 주기적으로 조회해 Kafka로 발행 후 `PUBLISHED`/`FAILED`로 갱신한다.
- `status = PUBLISHED`이고 **생성 후 7일이 지난 행**은 배치로 일괄 삭제한다 (`OutboxEventService`, `threshold = now().minusDays(7)`).

```
outbox                          -- 스키마별로 동일 구조 반복 (order/payment/inventory)
  id            BIGINT PK AUTO
  event_id      CHAR(36)  -- UUID, 통합 이벤트의 멱등키 → 소비자 Inbox 키
  aggregate_type VARCHAR  -- 'order' 등
  aggregate_id  VARCHAR   -- orderNumber (상관관계 키, 파티션 키)
  event_type    VARCHAR   -- 'OrderCreated' 등 (shared 계약명)
  topic         VARCHAR   -- 발행 대상 토픽
  payload       JSON      -- 통합 이벤트 직렬화 본문
  status        VARCHAR   -- CREATED / PUBLISHED / FAILED
  occurred_at   DATETIME
  created_at / updated_at  -- BaseTimeEntity 공통 컬럼
```

### 6.2 Inbox

- 소비자는 처리 전 `inbox`에 `event_id`를 먼저 저장(unique 제약)하고, `status`가 `CREATED`/`FAILED`인 행만 폴링 스케줄러(1초 주기)가 처리 대상으로 조회한다 — at-least-once 수신을 effectively-once 처리로 바꾼다.
- 처리 성공 시 `PROCESSED`, 처리 중 예외 발생 시 `FAILED`로 남아 다음 폴링에서 재시도된다 (ADR-0003 §4).
- 현재 별도 만료/정리(TTL) 배치는 없다.

```
inbox                            -- 스키마별로 동일 구조 반복 (order/payment/inventory)
  id             BIGINT PK AUTO
  event_id       CHAR(36) UNIQUE  -- 중복 소비 차단 키
  aggregate_type VARCHAR
  event_type     VARCHAR
  payload        LONGTEXT
  status         VARCHAR   -- CREATED / PROCESSED / FAILED
  created_at / updated_at  -- BaseTimeEntity 공통 컬럼
```