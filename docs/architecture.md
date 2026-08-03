# architecture — 모듈 / 패키지 구조 (v1.0)

> **목적**: 멀티모듈 + 헥사고날 아키텍처의 물리 구조와 의존 규칙을 확정한다.

---

## 1. 전체 구조 (Modular Monolith)

3개 바운디드 컨텍스트(order · payment · inventory) + 공통(shared) + 실행 모듈(bootstrap)  
= **5개 Gradle 모듈**  
**단일 배포 단위**지만 컨텍스트 모듈 경계로 마이크로서비스 수준의 격리를 강제한다.

```
order-platform/
├── bootstrap/                  ← Spring Boot 실행 모듈 (main, 전 컨텍스트 조립·설정)
├── shared/                     ← 통합 이벤트 계약 (Kafka 메시지 스키마)
├── order/                      ← Order 컨텍스트 (단일 모듈)
│   └── src/main/java/…/order/
│       ├── domain/             ← 순수 도메인
│       ├── application/        ← 유스케이스, 포트(interface)
│       └── infrastructure/     ← 어댑터: JPA · Kafka · REST · Outbox/Inbox
├── payment/                    ← Payment 컨텍스트
└── inventory/                  ← Inventory 컨텍스트
```

> (참고) bootstrap 모듈을 추가한 이유  
모듈러 모노리스는 단일 실행 파일로 뜬다. 전 컨텍스트 모듈을 조립하고 `@SpringBootApplication`을 두는 *실행 전용* 모듈

---

## 2. 헥사고날 레이어 (컨텍스트당 단일 모듈, 패키지로 분리)

각 컨텍스트 모듈 안에서 3계층을 패키지로 나눈다. 패키지 루트: `com.flab.orderplatform.{context}.{layer}`.

| 패키지(layer) | 책임                                                     | 의존 가능 대상 | 프레임워크                                        |
|------|--------------------------------------------------------|----------------|----------------------------------------------|
| `..domain` | Aggregate · VO · 도메인 이벤트 · 불변식 · 도메인 서비스               | **`..shared.event..`(통합 이벤트 계약)만** — 그 외 없음 (C-4) | **영속화 매핑까지만** (JPA·Hibernate·Spring Data 감사) |
| `..application` | 유스케이스, 포트, 트랜잭션 경계                                     | 같은 컨텍스트 domain, shared |                    |
| `..infrastructure` | JPA 영속 어댑터, Kafka 컨슈머/프로듀서, REST 컨트롤러, Outbox/Inbox 구현 | 같은 컨텍스트 application·domain, shared | Spring Boot·JPA·Kafka                      |

**의존 방향 (안쪽으로만)**:
```
infrastructure ──▶ application ──▶ domain
        └──────────────────────────▶ domain (어댑터가 도메인 직접 참조 가능)
domain ──▶ shared.event (통합 이벤트 계약 record 만, C-4)
```

포트 패턴:
- **인바운드 포트** = 유스케이스 interface (application). REST 컨트롤러·Kafka 컨슈머(infra)가 호출.
- **아웃바운드 포트** = repository·event publisher interface (application). JPA·Kafka 어댑터(infra)가 구현.

---

## 3. 모듈 간 통신 규칙 (핵심 불변식)
- 컨텍스트 간 통신은 카프카로만 가능하며, 바운디드 컨텍스트간 의존은 금지한다. 

---
## 4. shared 모듈
- 바운디드 컨텍스트간 이벤트 인터페이스만 포함한다.
  - 메시지 메타(`eventId`·`eventType`·`aggregateType`·`occuuredAt`)는 **Kafka 헤더**로 싣고, body 는 순수 payload 만 담는다.

---
## 5. 빌드 (Gradle 멀티모듈)

- 루트 `settings.gradle`에 5개 모듈(`shared`·`order`·`payment`·`inventory`·`bootstrap`) 등록
- 루트 `build.gradle`에 공통 규약(toolchain·BOM·테스트)
- 버전 카탈로그(`gradle/libs.versions.toml`)로 의존성 버전 중앙 관리.