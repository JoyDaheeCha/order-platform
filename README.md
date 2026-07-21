# order-platform

디자이너 브랜드 오픈마켓의 **B2C 주문 도메인**을 다루는 학습용 프로젝트.
구매자가 상품을 주문하면 **결제·재고가 분산 트랜잭션(Saga)으로 정합성을 맞추는** 흐름을, Kafka 기반 이벤트 드리븐 아키텍처(EDA)와 DDD·헥사고날·모듈러 모노리스로 구현한다.

> 이 레포는 제품 출시가 아니라 **개발 역량 강화**가 목적이다. 모든 설계 결정은 트레이드오프와 함께 [ADR](docs/adr)에 기록한다.

---

## 학습 목표

1. **Kafka / EDA / DDD 깊이 이해** — 동작 원리와 트레이드오프까지 명시적으로.
2. **분산 시스템 패턴 완벽 이해** — Saga 보상 트랜잭션, 멱등성, Outbox/Inbox, 재고 동시성 제어를 운영 시 주의점까지.

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| 언어 / 런타임 | Java 21 (Gradle toolchain 고정) |
| 프레임워크 | Spring Boot 3.4.x |
| 메시징 | Apache Kafka (Spring Kafka) |
| 영속화 | MySQL (컨텍스트별 스키마 분리) · Spring Data JPA |
| 캐시 / 동시성 | Redis (재고 동시성 어댑터 옵션) |
| 빌드 | Gradle 멀티모듈 (Groovy DSL) + 버전 카탈로그 |
| 경계 강제 | ArchUnit |

---

## 아키텍처 개요

**Modular Monolith with Boundaries** — 단일 배포 단위지만, 바운디드 컨텍스트 모듈 경계로 마이크로서비스 수준의 격리를 강제한다.

```
order-platform/
├── bootstrap/      ← Spring Boot 실행 모듈 (@SpringBootApplication, 전 컨텍스트 조립)
├── shared/         ← 통합 이벤트 계약 (Kafka 메시지 스키마 + EventEnvelope)
├── order/          ← Order 컨텍스트 — Saga 시작점
├── payment/        ← Payment 컨텍스트 — Saga 참여자 (가짜 PG)
└── inventory/      ← Inventory 컨텍스트 — 재고 동시성 제어
```

각 컨텍스트 모듈 내부는 헥사고날 3계층을 **패키지**로 나눈다 (`com.flab.orderplatform.{context}.{layer}`):

| 패키지 | 책임 | 프레임워크 |
|--------|------|-----------|
| `..domain` | Aggregate · VO · 도메인 이벤트 · 불변식 | ❌ 순수 Java (Spring/JPA 금지) |
| `..application` | 유스케이스 · 포트(in/out interface) · 트랜잭션 경계 | ⚠️ 트랜잭션 추상만 |
| `..infrastructure` | 어댑터 — JPA · Kafka · REST · Outbox/Inbox | ✅ Spring Boot · JPA · Kafka |

의존 방향은 안쪽으로만: `infrastructure → application → domain`.

## 외부도메인 관리법
persistence, domain 내에서 externals 패키지를 두고 해당 패키지 하위에서 관리한다.
예. 상품 도메인(product) 에서 동기화되는 데이터일 경우`externals.ProductEntity`
### 핵심 불변식

- **C-1** 바운디드 컨텍스트 간 컴파일 의존 금지 — `order`는 `payment`·`inventory`를 import 불가 (모듈 경계로 컴파일타임 강제).
- **C-2** 컨텍스트 간 통신은 **Kafka 통합 이벤트 only** (직접 호출·공유 테이블 금지).
- **C-3** 통합 이벤트 계약은 `shared`에만 정의.
- **C-4** 도메인 이벤트 ≠ 통합 이벤트 — infrastructure가 도메인 이벤트를 `shared` 통합 이벤트로 변환해 Outbox 발행.

> 레이어 규칙(domain↛infra 등)은 같은 모듈/클래스패스라 컴파일러가 막지 못한다. `bootstrap` 테스트의 **ArchUnit(`ModuleBoundaryTest`)** 이 강제한다.

자세한 물리 구조·의존 규칙은 [docs/architecture.md](docs/architecture.md) 참고.

---

## 주문 Saga 흐름 (코레오그래피)

중앙 오케스트레이터 없이 각 컨텍스트가 이벤트를 구독·발행하며 진행한다 ([ADR-0001](docs/adr/0001-saga-orchestration-vs-choreography.md)).

```
[PENDING] ──결제완료──> [PAID] ──재고차감──> [CONFIRMED]
    │                     │
    │                     └─ 재고부족(E2) ─> [CANCELLED] (결제 환불 보상)
    └─ 결제실패(E1) / 사용자취소(E6) ───────> [CANCELLED]
```

**Happy path**: 주문 생성(`OrderPlaced`) → 즉시 `202 Accepted` 응답 → 결제(`PaymentCompleted`) → 재고 차감(`StockDeducted`) → `OrderConfirmed`. 구매자는 `GET /orders/{id}`로 상태를 폴링한다 ([ADR-0006](docs/adr/0006-inbound-api-response-and-idempotency.md)).

분산 패턴 학습 포인트:

| 패턴 | 내용 |
|------|------|
| 보상 트랜잭션 | 결제 성공 후 재고 부족 시 환불(`PaymentRefunded`), 사용자 취소 |
| 멱등성 (생산자) | 같은 멱등키 중복 주문 방어 — 전용 `idempotency_keys` 테이블 |
| 멱등성 (소비자) | Inbox로 동일 이벤트 1회만 처리 |
| 재고 동시성 | 동시 주문 오버셀 방지 — 비관/낙관/원자적 UPDATE/Redis 4종 어댑터 비교 ([ADR-0002](docs/adr/0002-inventory-concurrency.md)) |

스코프·페르소나·상태 정의 전문은 [docs/product-spec.md](docs/product-spec.md), 도메인 규칙·불변식은 [docs/policy.md](docs/policy.md) 참고.

---

## 빌드 & 실행

요구 사항: 로컬에 JDK 21이 없어도 Gradle toolchain(foojay)이 자동 프로비저닝한다.

### 1. 인프라 기동 (앱 실행 전 **필수**)

```bash
docker compose up -d   # MySQL · Kafka · Redis 3종 기동
docker compose ps      # 3개 모두 (healthy) 인지 확인
```
### 2. 빌드 & 실행

```bash
# 전체 빌드 + 테스트 (ArchUnit 경계 검증 포함)
./gradlew build

# 모듈 경계 규칙만 검증
./gradlew :bootstrap:test

# 애플리케이션 실행 (실행 가능한 jar는 bootstrap 모듈에만)
./gradlew :bootstrap:bootRun
```
### 인프라 정지

```bash
docker compose down      # 정지 (데이터 유지)
docker compose down -v   # 정지 + 볼륨 삭제 (스키마 init SQL 을 다시 돌리고 싶을 때)
```

---

## 설계 결정 기록 (ADR)

| ADR | 결정 |
|-----|------|
| [0001](docs/adr/0001-saga-orchestration-vs-choreography.md) | Saga — 코레오그래피 채택 |
| [0002](docs/adr/0002-inventory-concurrency.md) | 재고 동시성 — `StockDeducer` 포트 + 4어댑터, 기본 원자적 UPDATE |
| [0003](docs/adr/0003-order-deadline-checker.md) | Order 데드라인 체커 · 재시도/DLQ |
| [0004](docs/adr/0004-schema-separation-outbox-readmodel.md) | 스키마 분리 · Outbox/Inbox · 주문 상태 read model |
| [0005](docs/adr/0005-hexagonal-layer-as-package-vs-module.md) | 헥사고날 레이어를 패키지로 (컨텍스트당 단일 모듈) |
| [0006](docs/adr/0006-inbound-api-response-and-idempotency.md) | 인바운드 주문 API — `202`+폴링 & 멱등키 저장 |
| [0007](docs/adr/0007-integration-event-contract.md) | shared 통합 이벤트 계약 — `EventEnvelope` · 3토픽 · 가산 버저닝 |

---

## 문서 맵
- [docs/design.md](docs/design.md) — **설계 논의용** · 설계 결정·트레이드오프 서사 (왜 이렇게 결정했나)
- [docs/product-spec.md](docs/product-spec.md) — 기획서 (페르소나 · 스코프 · 유저 플로우)
- [docs/policy.md](docs/policy.md) — 도메인 규칙 · 불변식 (정책서)
- [docs/architecture.md](docs/architecture.md) — 모듈/패키지 물리 구조 · 의존 규칙 (어떻게 구성했나)
- [docs/adr/](docs/adr) — 아키텍처 결정 기록
