# order-platform
<img width="758" height="566" alt="스크린샷 2026-09-17 오후 5 21 38" src="https://github.com/user-attachments/assets/25f691ab-086d-4d18-8c34-9d7e542f31b1" />


## 1. 프로젝트 소개
- **B2C 주문 도메인**
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

## 2. 프로젝트 주요 기술

### 1.1 Kafka / EDA / DDD
### 2.2 분산 시스템 
   - Saga 보상 트랜잭션
     - 결제 성공 후 재고 부족 시 환불
     - 사용자 취소
   - 멱등성 
     - 주문 생성시 멱등키로 중복 요청 막기
   - Outbox 패턴, Inbox 패턴
     - outbox: 발행시 최소 1번은 발행되도록 보장한다
     - inbox: 컨슈머의 멱등성을 보장한다. 
   - 재고 동시성 제어
     - 동시 주문 방지(오버셀 방지) — 재고 선점/차감/원복에 Redis 분산락(`@DistributedLock`, Redisson) 적용
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

각 컨텍스트 모듈 내부는 도메인/애플리케이션/인프라 3개의 **패키지**로 나눕니다.  
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
[RESERVING_INVENTORY] ──재고선점 성공──> [PENDING_PAYMENT] ──결제완료──> [PAID] ──재고차감──> [CONFIRMED]
         │                                     │
         │                                     └─ 결제실패 / 선점 타임아웃(10분) ─┐
         └─ 재고부족 ─────────────────────────────────────────────────────────┴─> [ORDER_FAILED]
```
### 5.2 Happy case

주문 생성(`OrderCreated`)   
→ api에서 즉시 `202 Accepted` 응답  
→ 재고 선점(`InventoryReserved`)  
→ 결제 준비 완료(`OrderPaymentPrepared`)  
→ 결제(`PaymentCompleted`)  
→ 결제 완료 처리(`OrderPaid`)  
→ 재고 차감(`InventoryDecreased`)   
→ 주문 확정 (`CONFIRMED`로 상태 전이)  
구매자는 `GET /order/{orderNumber}`로 상태를 확인할 수 있습니다. ([ADR-0006](docs/adr/0006-inbound-api-response-and-idempotency.md)).
### 5.3 상세설명
- 스코프·페르소나·유저플로우·상태 정의 : [docs/product-spec.md](docs/product-spec.md)
- 도메인 규칙·설계 결정 히스토리 : [docs/adr/](docs/adr)

---

## 6. 빌드 & 실행 방법

### 6.1 인프라 기동 (앱 실행 전 **필수**)

```bash
docker compose down -v # 기존에 기동중인 볼륨이 있을 때 초기화
docker compose up -d   # MySQL · Kafka · Redis 기동
docker compose ps      # mysql · kafka · redis 3개가 (healthy) 인지 확인
docker logs order-platform-mysql 2>&1 | grep -iE "ERROR|initdb"   # docker/mysql/init 하위 DDL 모두 실행 되었는지 확인
```
### 6.2 Kafka UI 접속
`docker compose up -d` 에 이미 포함되어 있어 별도 기동이 필요 없습니다.

| 항목 | 값 |
|------|-----|
| 주소 | **http://localhost:18080** |
| 클러스터명 | `local-cluster` |
| 로그인 | 없음 (로컬 전용) |

```bash
open http://localhost:18080          # macOS 기준 브라우저 열기
docker logs -f order-platform-kafka-ui   # 접속이 안 될 때 기동 로그 확인
```

### 6.3 빌드 & 실행

```bash
# 전체 빌드 + 테스트 (ArchUnit 경계 검증 포함)
./gradlew build

# 모듈 경계 규칙만 검증
./gradlew :bootstrap:test

# 애플리케이션 실행 (실행 가능한 jar는 bootstrap 모듈에만)
./gradlew :bootstrap:bootRun
```
### 6.4 인프라 정지

```bash
docker compose down      # 정지 (데이터 유지)
docker compose down -v   # 정지 + 볼륨 삭제 (스키마 init SQL 을 다시 돌리고 싶을 때)
```

### 6.5 API 규격 문서
http://localhost:8080/docs/index.html

### 6.6 부하 테스트 (k6)

```bash
# 시드 데이터 적재 (앱 실행 전, mysql 컨테이너가 healthy 여야 함)
mysql -h 127.0.0.1 -P 13306 -uroot -p<비밀번호> order_schema     < performance-test/k6/seed-product.sql
mysql -h 127.0.0.1 -P 13306 -uroot -p<비밀번호> inventory_schema < performance-test/k6/seed-inventory.sql

# 주문 생성(POST /order) 시나리오 실행 (기본 대상: http://localhost:8080)
k6 run performance-test/k6/order-create-scenario.js
# 다른 서버를 대상으로 실행할 때
BASE_URL=http://localhost:8080 k6 run performance-test/k6/order-create-scenario.js
```
결과 리포트는 `performance-test/k6/results/summary.html` · `summary.json`으로 저장

### 테스트 결과
p95: 10.47ms / avg: 6.72ms

---

## 7. 참고 문서
- [docs/architecture.md](docs/architecture.md) — 모듈/패키지 구조
- [docs/product-spec.md](docs/product-spec.md) — 기획서(정책서)
- [docs/adr/](docs/adr) — 설계 결정 히스토리
