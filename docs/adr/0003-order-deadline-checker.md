# ADR-0003: 주문 타임아웃 · 예외처리방법

- **상태**: Accepted (2026-06-27)
---

## 1. 맥락 

- 코레오그래피에는 "결제 이벤트가 안 온다"를 지켜보는 중앙감시자가 없다.

## 2. 요구사항과 구현법
### 2.1 요구사항
- Saga 각 단계는 타임아웃을 가지며, 이를 넘으면 실패로 간주하고 보상 처리를 한다.
- 비즈니스 예외와 인프라 이슈로 인한 일시적 예외를 구분지어 처리해야한다 (예외 타입에 따라 보상 이벤트 발행할지, 아니면 재시도할지 결정필요)
## 2.2 구현방법
- 타임아웃
  - order_saga_process에 데드라인 날짜를 둔다.
  - 스케줄러로 폴링하며 타임아웃여부 체크 (주문 컨텍스트)
- 예외
  - 인프라
    - 재시도후, 지속적 실패시 메시지 컨슈밍 실패 로그로 저장
    - 개발자가 수동 재시도
  - 비즈니스
    - 즉각적으로 보상 이벤트 발행

---

## 3. 결정: 주문타임아웃 확인 로직
### 3.1 타임아웃 기준  
- "`PENDING`으로 들어온 주문이 N초 안에 종료상태(`CONFIRMED`/`CANCELLED`)에 도달"하지 않으면 타임아웃

#### 3.2 타임아웃 구현 방법
- `order_saga_progress`에 `deadline_at`, `version`추가
- 스케줄러로 체크 

```
order_saga_progress   -- order_schema 소유 (ADR-0004)
  order_id          VARCHAR PK
  status            VARCHAR   -- PENDING/PAID/CONFIRMED/CANCELLED
  payment_completed DATETIME NULL
  stock_deducted    DATETIME NULL
  deadline_at       DATETIME      -- ★ 추가: OrderCreated 시 now()+ 타임아웃 시간
  version           BIGINT        -- ★ 추가: 낙관적 락 (타임아웃과 정상로직 동시 접근시 사용. - 예. 타임아웃 시간 만료 시점에 PaymentCompleted 바로 도착)
  updated_at        DATETIME
```
```java
// infrastructure
@Scheduled(fixedDelayString = "${order.saga.sweep-interval:5s}")
void sweep() {
    for (var id : repo.findExpired(PENDING, now()))   // status=PENDING AND deadline_at < now()
        timeoutUseCase.handle(id);                    // 인바운드 포트
}

// application
@Transactional
void handle(orderId) {
    var p = repo.find(orderId);
    if (p.status != PENDING) return;        // ★ 멱등 가드 — 이미 종료/이전 스캔 처리분 no-op
    p.markTimedOut();                        // PENDING → CANCELLING
    repo.save(p);                            // 낙관적 락(version)으로 경합 시 한쪽만 성공
    outbox.append(new OrderCancellationRequested(orderId, TIMEOUT)); // 보상-개시 이벤트(ADR-0007)
}
```

### 3.3 구현시 주의사항

- **정상 이벤트와 타임아웃 동시발생 가능성**
  - 스케줄러가 "만료"로 판단한 직후 `PaymentCompleted`가 도착할 수 있다
  - 정상 진행과 타임아웃이 **같은 행을 두고 경쟁** → 낙관적 락(`version`) 사용

### 3.4 타임아웃 발생시 정책: 주문 취소요청 실행

`OrderCancellationRequested(reason=TIMEOUT)` 발행 

```
OrderCancellationRequested(TIMEOUT) ─▶ [Payment] 결제됐으면 RefundPayment
              ─▶ [Inventory] 차감됐으면 RestoreStock
              ─▶ [Order] status = CANCELLED
```

---

## 4. 결정 : 비즈니스 실패와 인프라 오류 처리방법

### 4.1 처리방법

| 부류 | 예                     | 처리                                                     |
|------|-----------------------|--------------------------------------------------------|
| **비즈니스 실패** | 재고부족·결제거절 | 재시도 X → **즉시 실패 이벤트 발행 → 보상**                          |
| **일시적 인프라 오류** | DB 순단, 네트워크 타임아웃      | 지수 백오프 재시도 → 한계 초과 시 Message_failure_log 로 저장(DLQ 사용x) |

### 4.2 처리방법별 상세 설명
- 비즈니스 실패는 **예외로 던지지 않고** 핸들러가 정상 리턴하며 실패 이벤트를 발행한다.
- 인프라 오류만 예외로 터뜨려, 컨슈밍 실패 로그 (message_failure_log) 테이블에 저장한다.
  - 예. 재고부족은 재시도해도 소용이 없으므로 비즈니스 실패로 보고 즉시 실패처리해야한다.