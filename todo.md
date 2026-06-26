# order-platform

디자이너 브랜드 오픈마켓의 B2C 주문 도메인을 다루는 학습용 프로젝트.

## 프로젝트 개요

| 항목 | 내용 |
|------|------|
| **이름** | order-platform |
| **스택** | Java 21 · Spring Boot 3.x · Kafka · MySQL · Redis |
| **컨셉** | 디자이너 브랜드 오픈마켓 (B2C 주문 도메인) |
| **구조** | Modular Monolith with Boundaries · 헥사고날 아키텍처 · 멀티모듈 |

## 프로젝트 구조 (안)

```
order-platform/
├── order/                      ← 주문 핵심, Saga 시작점
│   ├── order-domain/           (순수 도메인)
│   ├── order-application/      (유스케이스, 포트)
│   └── order-infrastructure/   (JPA, Kafka, REST 어댑터)
├── payment/                    ← 결제 (Saga 참여자)
├── inventory/                  ← 재고 (동시성 제어)
├── shared/                     ← 공통 이벤트
└── docs/
    └── adr/                    ← 아키텍처 결정 기록
```

### 설계 원칙

- **모듈 간 통신**: Kafka 이벤트 only (직접 메서드 호출 금지)
- **DB**: 단일 MySQL, 모듈별 스키마 분리
- **경계 강제**: ArchUnit으로 모듈 경계 규칙 검증

## 학습 목표 (우선순위 순)

1. **Kafka / EDA / DDD 깊이 이해**
   회사에서 사용했지만 내부 동작과 트레이드오프를 제대로 이해하지 못했다는 자각이 있어 깊이 파고들고 싶다.

2. **분산 시스템 패턴 완벽 이해**
   보상 트랜잭션, 멱등성, Inbox 패턴 등 — 운영 시 주의할 점까지 명시적으로 학습한다.

3. **Spring Integration 회고**
   최근 회사에서 Spring Integration → `@KafkaListener` 마이그레이션 작업을 진행했다. 각각의 장단점을 회고하는 글을 작성한다.
