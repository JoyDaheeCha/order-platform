# architecture — 모듈 / 패키지 구조 (v1.0)

> **목적**: 멀티모듈 + 헥사고날 아키텍처의 물리 구조와 의존 규칙을 확정한다.
> 상위 결정: [policy.md](./policy.md), [ADR-0001 코레오그래피](./adr/0001-saga-orchestration-vs-choreography.md).

---

## 1. 전체 구조 (Modular Monolith)

3개 바운디드 컨텍스트(order · payment · inventory) + 공통(shared) + 실행 모듈(bootstrap) = **5개 Gradle 모듈**. **단일 배포 단위**지만 컨텍스트 모듈 경계로 마이크로서비스 수준의 격리를 강제한다.

```
order-platform/
├── bootstrap/                  ← Spring Boot 실행 모듈 (main, 전 컨텍스트 조립·설정)
├── shared/                     ← 통합 이벤트 계약 (Kafka 메시지 스키마)
├── order/                      ← Order 컨텍스트 (단일 모듈)
│   └── src/main/java/…/order/
│       ├── domain/             ← 순수 도메인 (프레임워크 의존 0)
│       ├── application/        ← 유스케이스, 포트(interface)
│       └── infrastructure/     ← 어댑터: JPA · Kafka · REST · Outbox/Inbox
├── payment/                    ← Payment 컨텍스트 (domain · application · infrastructure 패키지)
└── inventory/                  ← Inventory 컨텍스트 (동일 구조 + 재고 동시성)
```

> **모듈 단위 = 바운디드 컨텍스트**다. 헥사고날 3계층은 컨텍스트 모듈 *내부 패키지*로 둔다(레이어별 모듈 분리 X).
> **트레이드오프**: 컨텍스트 간 격리(C-1)는 모듈 경계로 *컴파일타임 강제*되지만, 레이어 규칙(A-1·A-3·A-6)은 같은 모듈/클래스패스 안이라 컴파일러가 못 막고 **ArchUnit이 강제**한다(§6). 레이어를 모듈로 쪼개는 강제력을 포기한 대신, 한 컨텍스트의 변경이 한 모듈 안에서 완결되는 응집도를 얻는다.

> **bootstrap 모듈을 추가한 이유**: 모듈러 모노리스는 단일 실행 파일로 뜬다. 전 컨텍스트 모듈을 조립하고 `@SpringBootApplication`을 두는 *실행 전용* 모듈이 필요하다. 도메인/애플리케이션 코드는 실행 책임을 갖지 않는다(todo.md 초안에 없던 모듈).

---

## 2. 헥사고날 레이어 (컨텍스트당 단일 모듈, 패키지로 분리)

각 컨텍스트 모듈 안에서 3계층을 패키지로 나눈다. 패키지 루트: `com.flab.orderplatform.{context}.{layer}`.

| 패키지(layer) | 책임 | 의존 가능 대상 | 프레임워크 |
|------|------|----------------|-----------|
| `..domain` | Aggregate · VO · 도메인 이벤트 · 불변식 · 도메인 서비스 | **없음** (순수 Java) | ❌ Spring/JPA 금지 |
| `..application` | 유스케이스(application service), **포트**(in/out interface), 트랜잭션 경계 | 같은 컨텍스트 domain, shared | ⚠️ 최소 (트랜잭션 추상만) |
| `..infrastructure` | 어댑터 — JPA 영속화, Kafka 컨슈머/프로듀서, REST 컨트롤러, Outbox/Inbox 구현 | 같은 컨텍스트 application·domain, shared | ✅ Spring Boot·JPA·Kafka |

> ⚠️ 세 계층이 **같은 모듈/클래스패스**에 있으므로 의존 방향을 컴파일러가 막지 못한다. A-1·A-3·A-6은 전적으로 **ArchUnit(§6)** 이 강제한다 — 레이어 분리 모듈을 포기한 대가다.

**의존 방향 (안쪽으로만)**:
```
infrastructure ──▶ application ──▶ domain
        └──────────────────────────▶ domain (어댑터가 도메인 직접 참조 가능)
domain ──▶ (아무것도 의존하지 않음)
```

포트 패턴:
- **인바운드 포트** = 유스케이스 interface (application). REST 컨트롤러·Kafka 컨슈머(infra)가 호출.
- **아웃바운드 포트** = repository·event publisher interface (application). JPA·Kafka 어댑터(infra)가 구현.

---

## 3. 모듈 간 통신 규칙 (핵심 불변식)

| 규칙 | 내용 |
|------|------|
| **C-1** | 바운디드 컨텍스트 간 **컴파일 의존 금지**. `order`는 `payment`·`inventory` 패키지를 import MUST NOT. |
| **C-2** | 컨텍스트 간 통신은 **Kafka 통합 이벤트 only** (직접 메서드 호출·공유 DB 테이블 금지). |
| **C-3** | 통합 이벤트 계약은 **`shared`에만** 정의한다. 각 컨텍스트는 `shared`를 의존해 발행·구독. |
| **C-4** | **도메인 이벤트 ≠ 통합 이벤트**. domain은 내부 도메인 이벤트만 안다. infrastructure가 도메인 이벤트 → `shared`의 통합 이벤트로 변환해 Outbox 발행한다. | 

> C-4가 중요한 학습 포인트: 도메인을 Kafka 메시지 포맷(통합 이벤트)으로부터 격리한다. 도메인은 "주문이 확정됐다"는 사실만 알고, 그게 어떤 JSON으로 어떤 토픽에 나가는지는 모른다.

---

## 4. shared 모듈

- **포함**: 통합 이벤트 계약(`OrderPlaced`, `PaymentCompleted`, `StockShortage` … payload record), 공통 메타 봉투 `EventEnvelope<T>`(`eventId`·`occurredAt`·`orderId`·`eventType` — 정책 PI-4), 토픽 이름 상수(`<context>.events`).
- **불포함**: 비즈니스 로직, Aggregate, 컨텍스트별 정책, **클라이언트 멱등키**(인바운드 경계 = ADR-0006 소관). (shared가 비대해지면 "분산된 모놀리스"가 됨 — 계약만 둔다.)
- 계약 상세(이벤트 목록·envelope·토픽·파티션·버저닝): [ADR-0007](./adr/0007-integration-event-contract.md).

---

## 5. 코레오그래피 구성요소의 위치 (ADR-0001 반영)

| 구성요소 | 위치 | 정책 |
|----------|------|------|
| 이벤트 컨슈머 (+ Inbox 중복제거) | 각 컨텍스트 `..infrastructure` 패키지 | PI-5 |
| 이벤트 프로듀서 (+ Outbox) | 각 컨텍스트 `..infrastructure` 패키지 | PI-6 |
| **Order 데드라인 체커** (타임아웃 감지) | `order.infrastructure` | PT-1 → ADR-0003 |
| **주문 진행도 추적 / 상태 read model** | `order.application` + `order.infrastructure` | PC-4 → ADR-0004 |
| 보상 반응 핸들러 (실패 이벤트 구독) | 각 컨텍스트 `..infrastructure` → application 호출 | PB-* |

> 중앙 오케스트레이터가 없으므로 "주문이 어디까지 진행됐나"는 order 컨텍스트가 수신 이벤트로 자체 추적한다(§ADR-0001 §5 주의).

---

## 6. 경계 강제 — ArchUnit 규칙

`bootstrap` 테스트(`ModuleBoundaryTest`)에서 전 모듈을 한 클래스패스로 임포트해 검증한다. 컨텍스트당 단일 모듈이라 레이어 규칙(A-1·A-3·A-6)의 강제는 *전적으로* 여기에 달려 있다.

- **A-1** `..domain` 패키지는 Spring·JPA·Kafka·jakarta.persistence를 의존 MUST NOT.
- **A-2** `..domain` 패키지는 같은 컨텍스트의 application·infrastructure를 의존 MUST NOT.
- **A-3** `..application` 패키지는 infrastructure를 의존 MUST NOT.
- **A-4** 컨텍스트 간 패키지 의존 MUST NOT (`..order..` → `..payment..` 금지) (C-1). ※ 이건 모듈 경계로 컴파일타임에도 막힌다.
- **A-5** 컨텍스트 간 유일한 공유는 `..shared..` 통합 이벤트뿐.
- **A-6** 레이어 의존 방향: infrastructure → application → domain (역방향 금지).

패키지 루트: `com.flab.orderplatform.{context}.{layer}` (예: `com.flab.orderplatform.order.domain`).

---

## 7. 빌드 (Gradle 멀티모듈)

- 루트 `settings.gradle`에 5개 모듈(`shared`·`order`·`payment`·`inventory`·`bootstrap`) 등록, 루트 `build.gradle`에 공통 규약(toolchain·BOM·테스트). 빌드 스크립트는 **Groovy DSL**(`b6904b2`에서 Kotlin DSL → Groovy 전환).
- Spring Boot 플러그인(`bootJar`)은 **`bootstrap`에만** 적용. 나머지는 `java-library`(plain jar).
- 버전 카탈로그(`gradle/libs.versions.toml`)로 의존성 버전 중앙 관리.
- 컴파일 toolchain은 **Java 21** 고정(foojay 자동 프로비저닝). Gradle 데몬은 JDK 25 비호환 이슈로 `gradle/gradle-daemon-jvm.properties`에서 **JDK 17**로 고정.

---

## 8. 후속 ADR 연결

- **ADR-0002** ✅ Accepted: [재고 동시성 기법](./adr/0002-inventory-concurrency.md) → `inventory.infrastructure`에 `StockDeducer` 포트 + 4개 어댑터(비관/낙관/원자/Redis), 기본 B
- **ADR-0003** ✅ Accepted: [Order 데드라인 체커 · 재시도/DLQ](./adr/0003-order-deadline-checker.md) — 전체 사가 타임아웃 + DB 스위퍼(폴링) → `order.infrastructure`
- **ADR-0004** ✅ Accepted: [스키마 분리 · Outbox/Inbox 테이블 · 주문 상태 read model](./adr/0004-schema-separation-outbox-readmodel.md) — 컨텍스트별 스키마(단일 인스턴스) + 컨텍스트별 DataSource(B-2)
- **ADR-0005** ✅ Accepted: [헥사고날 레이어 — 별도 모듈 vs 패키지](./adr/0005-hexagonal-layer-as-package-vs-module.md) — 레이어는 패키지로(컨텍스트당 단일 모듈, 총 5모듈). §1·§2·§6 구조의 결정 근거
- **ADR-0006** ✅ Accepted: [인바운드 주문 API — 응답 모델 & 멱등키 저장](./adr/0006-inbound-api-response-and-idempotency.md) — `202`+폴링(A-1, read model 재활용) + 전용 `idempotency_keys` 테이블(B-1, 주문과 같은 트랜잭션)
- **ADR-0007** ✅ Accepted: [shared 통합 이벤트 계약](./adr/0007-integration-event-contract.md) — 이벤트 목록(+보상-개시 `OrderCancellationRequested`) · `EventEnvelope<T>` 봉투 · `<context>.events` 3토픽 · 파티션 키 `orderId` · 가산 버저닝. §4 shared 모듈의 계약 결정 근거
