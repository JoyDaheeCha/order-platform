# ADR-0002: 재고 차감 동시성 기법

- **상태**: Accepted (2026-06-27)

---

## 1. 맥락

- 재고 차감은 `PaymentCompleted` 수신 후 일어난다. 
- **동일 상품을 여러 구매자가 동시에 주문**하면 데이터 정합성이 깨지는 문제가 발생한다.
  - <u>오버셀 (oversell. 주문시스템에서 재고보다 더 많은 수량이 판매되는 초과 판매 현상) 발생</u>

```
[예시]
- 시나리오: 재고 1개 남음. 구매자 A·B가 동시에 차감 시도.
- A: SELECT qty=1 →  B: SELECT qty=1  →  A: UPDATE qty=0  →  B: UPDATE qty=0
- 결과: 실 재고는 1개지만 2개가 판매되는 이슈가 발생한다.
```

## 2. 요구사항
오버셀을 방지해라.

---

## 3. 오벌셀 방지 구현 방법 4가지

### 옵션 A — 비관적 락 (`SELECT … FOR UPDATE`)
차감 전 재고 행을 잠가 트랜잭션을 직렬화한다.  
JPA `@Lock(PESSIMISTIC_WRITE)`.
- 방식: **"충돌이 날 거라 가정. 미리 잠그고 진행"** (블로킹)
- 장점: 직관적, 강한 정합성
- 단점: 락 경합 시 처리량↓

### 옵션 D — 낙관적 락 (`@Version` + 재시도)
버전 컬럼으로 충돌을 *감지*하고 재시도한다.
```sql
UPDATE stocks SET qty=qty-:n, version=version+1 WHERE product_id=:id AND version=:read;
-- affected rows=0 → 그새 변경됨 → OptimisticLockException → 재시도
```
- 방식: **"충돌은 드물 거라 가정 → 일단 진행, 깨지면 재시도"** (논블로킹).
- 장점: 락 미점유로 경합 낮을 때 빠름. (비관락과 대조)
- 단점: **재고처럼 핫 아이템 경합이 몰리면 재시도 폭주** → 오히려 느려짐

### 옵션 B — 원자적 조건부 UPDATE (`WHERE qty >= n`) ✅ 운영 기본
```sql
UPDATE stocks SET qty = qty - :n WHERE product_id = :id AND qty >= :n;  -- affected rows=0 → 부족
```
- 방식: **검사와 차감이 한 원자 연산 → "충돌"이라는 개념 자체가 없음.**
- 장점: 
  - race 자체가 성립 안 함
  - 데드락 위험 낮고 락 점유 짧음
  - 단일 MySQL에 가장 자연스럽고 단순.
- 단점: 여러개의 행을 업데이트할 경우 한 트랜잭션에서 진행한다. 이때 트랜잭션이 실패하여 롤백되면 구체적으로 어느 데이터에서 문제가 생겼는지 알 수 없다.

### 옵션 C — Redis
- 방식: Redis 에서 카운터를 줄이고, 이에 성공할경우 DB를 비동기적으로 업데이트 (방안 1. DECR, 방안2. Lua)
- 장점: 매우 높은 처리량, 핫 아이템·선착순에 강함
- 단점: **Redis ↔ DB 이중 소스 정합성** 문제

---

## 4. 결정 (Decision Outcome)

**재고 차감을 아웃바운드 포트로 추상화하고, 네 기법을 어댑터로 구현해 비교한다.** 운영 기본은 **B**.

### 4.1 포트 추상화 (헥사고날 활용)
```
application:   interface StockDeducer { DeductResult deduct(OrderId, List<Line>); }   ← 아웃바운드 포트
                    ▲              ▲                 ▲                ▲
infrastructure: Pessimistic…   Optimistic…   AtomicUpdate…(default)  Redis…           ← 어댑터 4개
```
`@Profile`/설정으로 어댑터를 갈아끼우며 **동일 시나리오** 테스트한다.

## 5. 결과 / 영향
**긍정**
- 동시성 이슈 방지 방법을 4가지 방식 직접 체험해본다.
- 포트 추상화로 도메인 무변경 + 어댑터 교체 가능 → 헥사고날 설계가 실제로 작동함을 확인.
---