# order-platform

## 1. 프로젝트 소개
- **B2C 주문 도메인**을 다루는 학습용 프로젝트
  - 목표: 구매자가 상품을 주문, 취소할때 결제, 재고의 변경이 정합성이 맞아야합니다.
  - 구현방식
    - 분산 트랜잭션(Saga)
    - Kafka 기반 이벤트 드리븐 아키텍처(EDA)
    - DDD
    - 모듈러 모노리스
      - 추후 서비스가 커져 MSA 로 변경시 서비스 분리를 쉽게 할 수 있습니다.

### 1.1 참고
모든 설계 결정 히스토리는 [ADR](docs/adr)에 기록됩니다.

---

## 2. 프로젝트의 학습 목표

### 1.1 Kafka / EDA / DDD 이해
### 2.2 분산 시스템 구현 방식 학습 
   - Saga 보상 트랜잭션
     - 결제 성공 후 재고 부족 시 환불
     - 사용자 취소
   - 멱등성 
     - 주문 생성시 멱등키로 중복 요청 막기
   - Outbox 패턴, Inbox 패턴
     - outbox: 발행시 최소 1번은 발행되도록 보장한다
     - inbox: 컨슈머의 멱등성을 보장한다. 
   - 재고 동시성 제어
     - 동시 주문 방지 — 비관/낙관/원자적 UPDATE/Redis 4가지 방법으로 구현
     - 참고) 상세 문서 - [ADR-0002](docs/adr/0002-inventory-concurrency.md)
---

## 3. 기술 스택

| 분류 | 기술                                    |
|------|---------------------------------------|
| 언어| Java 21                               
| 프레임워크 | Spring Boot 3.4.x                     |
| 메시징 | Apache Kafka (Spring Kafka)           |
| 영속화 | MySQL (컨텍스트별 스키마 분리) · Spring Data JPA |
| 캐시 / 동시성 | Redis                                 |
| 빌드 | Gradle 멀티모듈 + 버전 카탈로그     |
| 경계 강제 | ArchUnit                              |

---

## 4. 아키텍처 개요

모듈러 모놀리식(Modular Monolith with Boundaries)  
— 단일 배포 단위지만, 바운디드 컨텍스트 모듈 경계로 마이크로서비스 수준의 격리를 강제합니다.

### 4.1 패키지 구조
```
order-platform/
├── bootstrap/      ← Spring Boot 실행 모듈 (@SpringBootApplication, 전 컨텍스트 조립)
├── shared/         ← 통합 이벤트 계약 (Kafka 메시지 스키마)
├── order/          ← 주문 도메인 — Saga 시작점
├── payment/        ← 결제 도메인 — PG 연동은 Fake 클래스로 구현
└── inventory/      ← 재고 도메인 — 재고 동시성 제어
```

각 컨텍스트 모듈 내부는 헥사고날 3계층을 **패키지**로 나눕니다.  
(`com.flab.orderplatform.{context}.{layer}`):

| 패키지 | 책임                                      | 프레임워크                       |
|--------|-----------------------------------------|-----------------------------|
| `..domain` | Aggregate · VO · 도메인 이벤트 · 불변식          | -                           |
| `..application` | 포트 (인터페이스) · 트랜잭션 경계                    | -                           |
| `..infrastructure` | 어댑터 — JPA · Kafka · REST · Outbox/Inbox | ✅ Spring Boot · JPA · Kafka |

### 4.2 의존성 규약
```
의존 방향은 안쪽으로만 허용합니다.   
`infrastructure → application → domain`.
```
- 바운디드 컨텍스트 간 의존은 금지
  - 예. `order`는 `payment`·`inventory`를 import 불가 
- 컨텍스트 간 통신은 **Kafka 통합 이벤트**만 허용
  - 메서드 직접 호출 하거나 컨텍스트간 테이블 JOIN 금지
  - 컨텍스트간 주고받는 메시지 인터페이스는 `shared`에만 정의

---

## 5. 사용자 시나리오
### 5.1 `주문` 상태값 변경 순서도
```
[PENDING] ──결제완료──> [PAID] ──재고차감──> [CONFIRMED]
    │                     │
    │                     └─ 재고부족(E2) ─> [CANCELLED] (결제 환불 보상)
    └─ 결제실패(E1) / 사용자취소(E6) ───────> [CANCELLED]
```
### 5.2 Happy case

주문 생성(`OrderCreated`)   
→ api에서 즉시 `202 Accepted` 응답  
→ 결제(`PaymentCompleted`)  
→ 재고 차감(`StockDeducted`)   
→ 주문 확정(`OrderConfirmed`)  
구매자는 `GET /orders/{id}`로 상태를 폴링합니다. ([ADR-0006](docs/adr/0006-inbound-api-response-and-idempotency.md)).
### 5.3 상세설명
- 스코프·페르소나·상태 정의 : [docs/product-spec.md](docs/product-spec.md)
- 도메인 규칙·불변식 : [docs/policy.md](docs/policy.md)

---

## 6. 빌드 & 실행 방법

### 6.1 인프라 기동 (앱 실행 전 **필수**)

```bash
docker compose down -v # 기존에 기동중인 볼륨이 있을 때 초기화
docker compose up -d   # MySQL · Kafka · Redis 3종 기동
docker compose ps      # 3개 모두 (healthy) 인지 확인
docker logs order-platform-mysql 2>&1 | grep -iE "ERROR|initdb"   # docker/mysql/init 하위 DDL 모두 실행 되었는지 확인
```
### 6.2 빌드 & 실행

```bash
# 전체 빌드 + 테스트 (ArchUnit 경계 검증 포함)
./gradlew build

# 모듈 경계 규칙만 검증
./gradlew :bootstrap:test

# 애플리케이션 실행 (실행 가능한 jar는 bootstrap 모듈에만)
./gradlew :bootstrap:bootRun
```
### 6.3 인프라 정지

```bash
docker compose down      # 정지 (데이터 유지)
docker compose down -v   # 정지 + 볼륨 삭제 (스키마 init SQL 을 다시 돌리고 싶을 때)
```

---

## 7. 참고 문서
### 7.1 설계 결정 히스토리 (ADR)

| ADR | 결정                                               |
|-----|--------------------------------------------------|
| [0001](docs/adr/0001-saga-orchestration-vs-choreography.md) | Saga — 코레오그래피 채택                                 |
| [0002](docs/adr/0002-inventory-concurrency.md) | 재고 동시성 — `StockDeducer` 포트 + 4어댑터, 기본 원자적 UPDATE |
| [0003](docs/adr/0003-order-deadline-checker.md) | Order 데드라인 체커 · 재시도/DLQ                          |
| [0004](docs/adr/0004-schema-separation-outbox-readmodel.md) | 스키마 분리 · Outbox/Inbox · 주문 상태 read model         |
| [0005](docs/adr/0005-hexagonal-layer-as-package-vs-module.md) | 헥사고날 레이어 구성법 (컨텍스트당 단일 모듈)                       |
| [0006](docs/adr/0006-inbound-api-response-and-idempotency.md) | 인바운드 주문 API — `202`+폴링 & 멱등키 저장                  |
| [0007](docs/adr/0007-integration-event-contract.md) | shared 통합 이벤트 계약 |

---

### 7.2 문서에서 확인 가능한  사항
- [docs/design.md](docs/design.md) — 설계 결정 히스토리
- [docs/product-spec.md](docs/product-spec.md) — 기획서 (페르소나, 스코프, 유저 플로우)
- [docs/policy.md](docs/policy.md) — 도메인 규칙, 불변식 (정책서)
- [docs/architecture.md](docs/architecture.md) — 모듈/패키지 물리 구조, 의존 규칙
- [docs/adr/](docs/adr) — 아키텍처 결정 기록
