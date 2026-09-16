# ADR-0006: 주문 API — 응답 모델 & 멱등키 저장

- **상태**: Accepted (2026-06-27, 2026-09-16 구현 반영 갱신)

---

## 1. 요구사항
- 주문 생성 응답 스펙 확정 필요 
- 주문 중복 생성 방지용 로직 추가 필요

---

## 2.결정 
### 2.1 인바운드 응답 모델
- `POST /order` — 주문 생성 응답으로 생성된 주문의 내부 PK(`Long id`)를 즉시 반환한다.
  - 구현: `OrderCommandController.createOrder()`가 `Long`을 그대로 리턴 (별도 `ResponseEntity`/DTO 래핑·`202 Accepted` 상태코드 지정 없음 → 기본 `200 OK`).
- 클라이언트는 **폴링**으로 최종 주문 상태 확인 (`GET /order/{orderNumber}` — `OrderQueryController.searchOrder()`)

---

### 2.2 멱등키 저장 메커니즘
- 요청 헤더 `Idempotency-key`로 멱등키를 전달받는다.
- 1차 방어: `@Idempotent` AOP(`IdempotentAspect`)가 Redis(Redisson)에 `idempotent:{key}` 버킷을 TTL(기본 1분, `idempotent.ttl` 설정)로 저장 — 동시 중복 요청은 먼저 온 쪽만 처리하고 나머지는 `DuplicatedRequestException` 또는 캐싱된 응답을 재사용한다.
- 2차 방어: `orders` 테이블에 `idempotent_key` UNIQUE 컬럼
