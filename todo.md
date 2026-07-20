# 프로젝트 요약
주문 플랫폼의 분산 환경 시스템을 구현

## 프로젝트 내 기능
- POST /orders 
- GET /orders/{id} 
- POST /orders/{id}/cancel

## 목적
- 도메인 유즈케이스는 단순화 하되, 분산환경에서 사용하는 다양한 기법 (예. SAGA 패턴, 동시성) 을 익힌다
- 부하테스트에서 트래픽을 충분히 견디도록 설계한다. 

# TODO
##  환경세팅
[x] 앱이 뜨고, MySQL·Kafka·Redis에 붙는다.
[x] 주문, 재고, 결제 컨텍스트마다 자기 스키마만 확인할 수 있음을 테스트로 증명한다. 

## 주문 생성(POST /orders) api 추가 7/19 구현 목표 (day3~day6)
[ ] 테이블 추가
    - [ ] order_item : 상품 id, 주문번호, 가격 snapshot (주문 당시 가격)
    - [ ] product : 상품 id, 가격 (상품 도메인에서 받아오는 상품 메타정보)
[ ] 주문을 생성하면 PENDING 주문이 DB에 저장된다
 - [x] prod
 - [ ] test
[ ] 주문 생성 중복 요청을 방지
    - [ ] 같은 멱등키 재요청은 기존 주문을 반환한다.
        - [ ] 멱등키 어떻게 만들지?
    - [ ] 같은 키에 다른 페이로드는 409로 거부한다. 
[ ] OrderPlaced(주문이 요청되었다) 이벤트가 발행된다

## 주문 조회 api(GET /orders/{id}) 추가

## 주문 취소 api 추가

# (참고) 세부 구현 사항
### Day 3 — Order 도메인 (순수 자바) `Phase 1`
**목표**: 프레임워크 0 의존의 Order Aggregate가 불변식을 지킨다.
> 📚 **완료 후 자문**: ① Aggregate·VO·불변식은 각각 무엇이고, 불변식은 어디서(생성자/팩토리) 강제해야 하나? ② VO의 불변성·값 동등성은 왜 필요한가? ③ 도메인이 Spring/JPA에 의존하면 구체적으로 뭐가 나빠지나?
- [ ] `Money` VO (KRW 정수, 소수/음수 금지, policy §0)
- [ ] `OrderLine` VO (수량 1~99, PO-2)
- [ ] `Order` Aggregate — 라인 1개↑(PO-1), 상품 중복 라인 거부(PO-3), 총액 스냅샷(PO-4)
- [ ] `OrderStatus` enum (이 단계는 `PENDING` 생성까지) + 도메인 이벤트 `OrderPlaced`(내부, C-4)
- [ ] 도메인 단위 테스트 (PO-1~4 불변식)
- **✅ 완료 기준**: 도메인 단위테스트 그린 + ArchUnit A-1(도메인 순수성) 통과

### Day 4 — Order application + 저장 어댑터 `Phase 1`
**목표**: 유스케이스가 주문을 생성해 DB에 저장한다.
> 📚 **완료 후 자문**: ① 인바운드 포트와 아웃바운드 포트는 무엇이 다른가? ② application이 인터페이스를 소유하고 infra가 구현하면(DIP) 의존 방향이 어떻게 뒤집히나? ③ 트랜잭션 경계를 왜 도메인이 아니라 유스케이스(application)에 두나?
- [ ] 인바운드 포트 `PlaceOrderUseCase`, 아웃바운드 포트 `OrderRepository`·`OrderEventPublisher`
- [ ] `PlaceOrderService` — 시드 데이터로 상품/가격 검증(PO-5), 트랜잭션 경계 설정
- [ ] `OrderEntity` 확장(Day 2의 최소 스캐폴드 → Day 3 Aggregate에 맞춘 진짜 매핑: 주문 라인·상태) + repository 어댑터 (order 스키마). 아웃바운드 포트 `OrderRepository`를 `OrderJpaRepository`가 뒤에서 구현한다
- [ ] **Flyway 도입** (Day 2에서 이동) — `flyway-core` + `flyway-mysql`(⚠️ Flyway 10부터 DB별 모듈 분리. 없으면 컴파일은 통과하고 런타임에 `Unsupported Database`. 이름도 `flyway-database-mysql`이 아니라 **`flyway-mysql`**), order의 Flyway 빈 1개(`locations: db/migration/order`), `@DependsOn`으로 EMF보다 **먼저** 실행
- [ ] **`V1__create_orders.sql`** — Flyway의 첫 마이그레이션이 더미가 아니라 진짜 테이블이다
- [ ] **Day 2 더미 정리** — `PingEntity` + `docker/mysql/init/02-ping.sql` 삭제. 로컬 DB는 `docker compose down -v`로 초기화(init 스크립트는 **최초 부팅에만** 돌아서 파일만 지우면 기존 컨테이너엔 테이블이 남는다. `validate`는 여분 테이블을 탓하지 않아 조용히 남는다)
- [ ] 상품 시드 데이터
- **✅ 완료 기준**: 서비스 테스트로 주문 생성→저장 확인, ArchUnit A-3(application→infra 금지) 통과
> 📌 **왜 Day 2가 아니라 지금 Flyway인가** — 오늘 처음으로 *살아남는* 테이블(`orders`)이 생기기 때문. Flyway의 가치는 "같은 스키마 변경을 여러 환경에 시간에 걸쳐 결정적으로 반복 적용"인데, Day 2엔 그 인스턴스가 0개였다(운영 DB·타 개발자·배포 전부 없음). 미루는 비용도 0이었다 — 지금 스키마는 새로 만드는 테이블 하나뿐이라 뜰 `baseline`이 없다. 레트로핏이 아니라 그냥 시작일이 이틀 늦은 것.
> 📌 **payment·inventory의 Flyway는 각자 첫 테이블이 생기는 D7·D8에.** 같은 이유(just-in-time). 그때까지 두 EMF는 엔티티가 0개라 `validate`할 것도 없다.
> 📌 `ddl-auto`는 오늘도 **`validate` 그대로**다. Day 2에 `create`를 안 쓴 덕에 D1~D16 내내 안 흔들리고, DDL의 주인만 `02-ping.sql`(스캐폴드) → Flyway(진짜)로 조용히 교대한다. 네이밍 전략도 Day 2에 이미 `validate`로 검증받았으니 오늘 Flyway의 snake_case SQL과 충돌하지 않는다.

### Day 5 — 인바운드 API + 멱등키 `Phase 1`
**목표**: `POST /orders`가 202를 주고, 중복 요청을 막는다.
> 📚 **완료 후 자문**: ① 클라이언트 멱등키는 무엇을 막나? 서버가 아니라 클라이언트가 키를 만드는 이유는? ② 왜 202를 주고 확정 응답을 안 주나(sync-over-async는 뭐가 문제)? ③ "멱등성도 원자성 문제"란 무슨 뜻인가 — 키 기록과 주문 생성이 분리되면 어떤 사고가 나나?
- [ ] `OrderController` — `POST /orders`(멱등키 헤더) → **`202 Accepted {orderId, PENDING}`** (ADR-0006 A-1)
- [ ] `idempotency_keys` 테이블 — **주문 INSERT와 같은 트랜잭션**(ADR-0006 B-1)
- [ ] 멱등 규칙 — 같은 키 재요청 시 기존 결과 반환(PI-2), 같은 키·다른 페이로드 → **409**(PI-3)
- **✅ 완료 기준**: curl로 202 확인 / 같은 키 2회 → 주문 1건 / 다른 페이로드 같은 키 → 409

---

# Week 2 — 첫 관통 → Saga Happy Path

### Day 6 — Outbox + read model → **첫 관통 완성** ⚠️ `Phase 1`
**목표**: 주문이 요청부터 Kafka 발행·조회까지 관통한다. (Phase 1 完)
> 📚 **완료 후 자문** ⭐: 
> ① dual-write 문제가 정확히 무엇이고, 왜 "DB 저장 + Kafka 발행"을 트랜잭션 하나로 못 묶나? 
> ② Outbox 패턴은 이걸 어떻게 푸나? 릴레이는 왜 별도로 도나? 
> ③ 스키마 분리 때문에 왜 read model이 필요해지나?
> ④ 도메인 이벤트와 통합 이벤트를 왜 나누나(C-4)? 안 나누면 뭐가 문제인가? *(D2에서 이동)*
> ⑤ Envelope에서 payload가 메타(eventId 등)를 "모르게" 하면 뭐가 좋은가? *(D2에서 이동)*
> > 📚 **멘토님 질문** ⭐:
> ① 실무에서 Kafka partition key 활용하는 방법
> ② orderId를 partition key로 사용했을 때 이점
- [ ] **`EventEnvelope<T>` 봉투 record** (ADR-0007 §4 옵션 c) — `eventId·occurredAt·orderId·eventType·payload`. Outbox 테이블 컬럼과 1:1 일치하는지 여기서 직접 확인
- [ ] **payload `OrderPlaced` 1종** (ADR-0007 §3) + 토픽 상수 `order.events` — 금액은 `long`(KRW 정수). `Money` VO는 order의 도메인 개념이라 shared에 넣으면 안 됨(C-3 위반)
- [ ] **Outbox 테이블 + 릴레이** — 상태변경과 이벤트 적재를 원자적으로(PI-6, dual-write 해결). 릴레이가 `EventEnvelope`로 포장해 `order.events` 발행
- [ ] **read model** `order_saga_progress` + `GET /orders/{id}` 폴링 조회 (ADR-0004, PC-4)
- **✅ 완료 기준**: `POST /orders` → `order.events` 토픽에 `OrderPlaced` 실제로 뜸 → `GET`으로 PENDING 조회 ✨ **첫 walking skeleton 관통**
> ⚠️ Outbox 릴레이(폴링 or `@TransactionalEventListener`)가 핵심 학습 포인트이자 리스크. 여유를 뒀다.
> 📌 **계약은 just-in-time.** ADR-0007 §3의 payload 10종을 한 번에 내리지 않는다 — 나머지는 각자의 **발행자가 생기는 날**에 추가한다(D7 `PaymentCompleted`·`PaymentFailed` / D8 `StockDeducted`·`StockShortage` / D9 `OrderConfirmed` / D11 `PaymentRefunded` / D12 `OrderCancellationRequested`·`OrderCancelled`·`StockRestored`). 계약 설계 자체는 ADR-0007에서 이미 끝났고, 여기서 하는 건 사용처가 생긴 만큼만 옮겨 적는 일이다.
> 📌 `eventType` 문자열↔타입 매핑은 **D7로** — 역방향(역직렬화 디스패치)이 실제로 필요해지는 첫 지점이 컨슈머다. 발행자 하나뿐인 D6에서는 정방향만 있으면 된다.

### Day 7 — Payment 컨텍스트 `Phase 2`
**목표**: 주문 이벤트를 받아 결제하고 결과를 발행한다.
> 📚 **완료 후 자문**: ① 코레오그래피 Saga에서 "중앙 조정자가 없다"는 게 구체적으로 어떤 구조인가? ② 컨슈머가 한 토픽에 섞인 여러 이벤트를 어떻게 분기·역직렬화하나(`eventType`)? ③ 결정론적 실패 주입이 확률 기반보다 테스트에 유리한 이유는?
- [ ] **payment Flyway 빈 + `V1__create_payments.sql`** (D4에서 예고) — payment의 첫 테이블이 오늘 생긴다. `ddl-auto`는 계속 `validate`
- [ ] Payment 도메인 — 결제 금액 = 주문 총액 일치(PP-1), 1회·전액(PP-2)
- [ ] `order.events` 구독 → `OrderPlaced` 수신 → 결제 시도
- [ ] **가짜 PG** — 금액 끝자리 `7`이면 `PaymentFailed`, 그 외 `PaymentCompleted`(PP-3, 결정론적)
- [ ] Outbox로 `payment.events` 발행
- **✅ 완료 기준**: `OrderPlaced` → `PaymentCompleted` 흐름 확인 (끝자리 7 주문은 `PaymentFailed`)

### Day 8 — Inventory 컨텍스트 `Phase 2`
**목표**: 결제 완료를 받아 재고를 차감한다.
> 📚 **완료 후 자문**: ① `WHERE qty >= n` 조건부 UPDATE는 왜 "충돌"이라는 개념이 없나? ② 왜 결제 *후*에 재고를 차감하나 — 순서를 바꾸면 보상 학습이 어떻게 달라지나(PV-2)? ③ all-or-nothing 차감(PV-3)은 왜 필요한가?
- [ ] **inventory Flyway 빈 + `V1__create_stocks.sql`** (D4에서 예고) — inventory의 첫 테이블이 오늘 생긴다. 이로써 Flyway 3세트 완성
- [ ] Stock 도메인 (물리 재고, 예약 개념 없음 PV-1) + **재고 시드**(셀러 없음)
- [ ] `payment.events` 구독 → `PaymentCompleted` 수신 → 재고 차감(PV-2: 결제 후 차감)
- [ ] all-or-nothing(PV-3), 기본 어댑터 = **원자적 조건부 UPDATE**(`WHERE qty >= n`, ADR-0002 기본 B)
- [ ] Outbox로 `StockDeducted`/`StockShortage` 발행
- **✅ 완료 기준**: `PaymentCompleted` → `StockDeducted` 흐름 확인

### Day 9 — Inbox + Order 상태 전이 → **Saga happy path 완성** `Phase 2`
**목표**: 주문이 PENDING→PAID→CONFIRMED까지 이벤트로 전진한다. (Phase 2 完)
> 📚 **완료 후 자문** ⭐: ① Kafka가 at-least-once면 중복 소비는 왜 필연인가? ② Inbox 패턴은 `eventId`로 무엇을 하고, effectively-once란 무슨 뜻인가? ③ 소비자 측 멱등(Inbox)과 생산자 측 멱등키(D5)는 무엇이 다른가? ④ 상태 전이를 멱등(no-op)하게 만드는 이유는(PS-4)?
- [ ] **Inbox 테이블 + 소비 가드** (전 컨텍스트 공통) — `eventId` 기준 1회만 처리(PI-5), effectively-once
- [ ] Order가 `PaymentCompleted`→`PAID`, `StockDeducted`→`CONFIRMED` 전이 (PS-1~4 불변식)
- [ ] `OrderConfirmed` 발행 + read model 진행도 갱신
- **✅ 완료 기준**: **E2E — 주문 1건이 `PENDING→PAID→CONFIRMED`, GET 폴링으로 확정 확인** ✨

### Day 10 — happy path 안정화 + 멱등 검증 `Phase 2`
**목표**: 정상 흐름을 테스트로 못 박고 디버깅 여유를 둔다.
> 📚 **완료 후 자문**: ① 비동기 이벤트 흐름은 동기 코드와 달리 무엇 때문에 테스트가 까다로운가(최종 일관성 대기)? ② 멱등성을 테스트로 어떻게 증명하나(같은 이벤트 2회 → 상태 1회 변화)? ③ 이 시스템의 멱등성은 몇 개 층위에 흩어져 있나(design.md §4)?
- [ ] Inbox 멱등 테스트 — 같은 이벤트 2회 전달 → 1회만 처리(PI-5, PS-4)
- [ ] happy path E2E 테스트 코드화
- [ ] Week 1~2 밀린 것 정리 / 버퍼
- **✅ 완료 기준**: happy path + Inbox 멱등 테스트 그린, Week 2까지 데모 가능한 상태

---

# Week 3 — 보상 · 타임아웃 · 동시성 · 검증

### Day 11 — 보상 트랜잭션 E1·E2 `Phase 3`
**목표**: 하류 실패 시 진행분을 역순 보상한다.
> 📚 **완료 후 자문** ⭐: ① 분산 트랜잭션에서 왜 2PC/롤백 대신 보상을 쓰나? ② 왜 "수행된 정방향 단계만 역순으로" 보상하나(PB-1)? ③ 보상 동작이 멱등하지 않으면 무슨 일이 생기나(PB-2)? ④ "환불은 롤백이 아니라 새로운 정방향 동작"이라는 말의 의미는?
- [ ] **E1 결제 실패** — `PaymentFailed` → `OrderCancelled` (재고 차감 전, 보상 없음 PC-2)
- [ ] **E2 재고 부족** — `StockShortage` → Payment 전액 환불(PP-4) → `PaymentRefunded` → `OrderCancelled` (진짜 보상)
- [ ] 보상 멱등(PB-2) — 이미 보상 시 no-op, 비즈니스 실패는 즉시 보상(재시도 금지, PB-4)
- **✅ 완료 기준**: 끝자리 7 주문 → CANCELLED / 재고부족 주문 → 환불 후 CANCELLED (돈·재고 정합)

### Day 12 — 보상 E6 (사용자 취소) `Phase 3`
**목표**: 사용자가 확정 전 취소하면 진행분이 보상된다. (Phase 3 完)
> 📚 **완료 후 자문**: ① 하류 실패(E1/E2)와 사용자 취소(E6)는 보상 트리거가 왜 다른가? ② terminal 이벤트(`OrderCancelled`)를 보상 트리거로 쓸 수 없는 이유는? ③ 취소 가능 여부를 왜 상태로 판정하나(CONFIRMED 후엔 왜 불가, PC-1)?
- [ ] `POST /orders/{id}/cancel` → `CancelOrder` (PENDING·PAID만 허용, CONFIRMED 후 거부 PC-1)
- [ ] `PENDING` 취소 → 단순 취소(PC-2) / `PAID` 취소 → **`OrderCancellationRequested(USER_CANCEL)`**(ADR-0007 A-1) → 환불 보상(PC-3)
- [ ] 보상-개시 이벤트 구독 토폴로지 (Payment/Inventory 진행분 역순 보상)
- **✅ 완료 기준**: PAID 상태 취소 → 환불 후 CANCELLED / CONFIRMED 후 취소 요청 → 거부(PC-1)

### Day 13 — 타임아웃: 데드라인 체커 ⚠️ `Phase 4`
**목표**: 결제가 안 오면 스스로 감지해 취소한다.
> 📚 **완료 후 자문** ⭐: ① 코레오그래피에서 "결제가 안 온다"를 아무도 못 보는 이유는? ② 폴링 스위퍼(DB 상태)가 인메모리 타이머보다 나은 점은 — 프로세스가 죽으면 무슨 차이가 나나? ③ Kafka엔 왜 지연 메시지를 직접 못 만드나? ④ 정상확정과 타임아웃취소가 경합하면 어떻게 한쪽만 이기게 하나(PC-4)?
- [ ] **폴링 스위퍼** — `order_saga_progress.deadline_at` 스캔(ADR-0003 결정 B, PT-1). DB 상태 기반이라 크래시 복원 가능
- [ ] **전체 사가 타임아웃** 30s(단계별 ❌, ADR-0003 결정 A) → `OrderCancellationRequested(TIMEOUT)` → Day 12 보상 흐름 재사용
- [ ] **정상확정 vs 타임아웃취소 경합** — 낙관적 락 + 멱등 가드로 한쪽만 승리(PC-4)
- **✅ 완료 기준**: 결제 이벤트 유실 시나리오 → 30s 후 타임아웃 취소 / 앱 재시작 후 밀린 만료 감지
> ⚠️ 코레오그래피의 "중앙 감시자 부재"라는 빚을 갚는 핵심 구간(design.md §3). 개념 이해에 시간 배분.

### Day 14 — 재시도 + DLQ `Phase 4`
**목표**: 일시 오류는 재시도하고, 영구 실패는 DLQ로 보낸다. (Phase 4 完)
> 📚 **완료 후 자문** ⭐: ① 어떤 오류는 재시도하고 어떤 오류는 즉시 보상하나 — 둘을 섞으면 무엇이 오염되나(ADR-0003 결정 C)? ② DLQ는 무엇이고 왜 "유실이 아니다"라고 하나? ③ 재시도의 전제가 멱등성인 이유는(PT-3)?
- [ ] 지수 백오프 재시도 — 일시적 오류(네트워크·DB 순단)만(PT-2), 전제는 멱등성(PT-3)
- [ ] DLQ + 경보 로그(학습용) — DLQ는 유실 아님, 재처리 경로(PT-4, PB-3)
- [ ] **비즈니스 실패 ↔ 인프라 오류 분리** — 재고부족을 재시도로 흘리면 정상결과가 DLQ 오염(ADR-0003 결정 C)
- **✅ 완료 기준**: 일시 오류 주입 → 재시도 후 성공 / 한계 초과 → DLQ 적재 확인

### Day 15 — 재고 동시성 4어댑터 ⚠️ `Phase 5`
**목표**: 포트 교체만으로 4가지 기법을 비교하고 오버셀 0을 증명한다.
> 📚 **완료 후 자문** ⭐: 
> ① race condition은 어떻게 오버셀을 만드나(read-modify-write)? 
> ② 비관적 락·낙관적 락·원자적 UPDATE·Redis 각각의 트레이드오프는(블로킹/재시도폭주/데드락/이중소스)? 
> ③ 도메인 무변경으로 어댑터만 갈아끼울 수 있다는 게 헥사고날의 무엇을 증명하나?
> > 📚 **멘토님께 받은 질문** ⭐:
> ① DB의 isolation level 별로 어떤 이상 현상이 발생할 수 있는지?
> ② isolation level 은 DB단에서 실제로 어떻게 구현이 되는지 ?
- [ ] `StockDeducer` 포트 + 어댑터 — A 비관적 락 / **B 원자적 UPDATE(Day 8에 이미)** / C 낙관적 락(@Version+재시도) / D Redis
- [ ] 프로파일/프로퍼티로 어댑터 교체 가능하게
- [ ] **공통 동시성 테스트 하니스** — 마지막 재고 1개에 동시 주문 N건 → 정확히 1건 성공, 오버셀 0(PV-4)을 4어댑터 동일 테스트로
- **✅ 완료 기준**: 4어댑터 전부 "오버셀 0" 통과, 트레이드오프(블로킹·재시도폭주·데드락·이중소스) 관찰 기록
> ⚠️ Redis 어댑터(D)가 리스크. 시간 부족 시 A·C·하니스 먼저 완성하고 D는 Day 16으로 넘김.

### Day 16 — 검증 종합 + 경계 강제 켜기 `Phase 6`
**목표**: 흩어진 시나리오를 통합 검증하고 경계 규칙을 조인다.
> 📚 **완료 후 자문**: ① 무엇을 컴파일러(모듈 경계)에 맡기고 무엇을 테스트(ArchUnit)에 맡기나(design.md §1 원칙 3)? ② 이벤트 순서는 어디까지 보장되나 — 토픽 간 전역 순서가 없는데 왜 문제가 안 되나(ADR-0007 §6)?
- [ ] **ArchUnit `allowEmptyShould(true)` 제거** — 도메인 코드가 생겼으니 A-1·A-2·A-3·A-6 실제 강제
- [ ] 멱등성 4층위 통합 테스트(클라이언트 멱등키·Inbox·상태전이·보상, design.md §4)
- [ ] 보상 시나리오 통합 테스트(E1·E2·E6·타임아웃 최종 정합)
- [ ] (스필오버 버퍼) Day 15 Redis 어댑터 / 밀린 항목 마무리
- **✅ 완료 기준**: 전체 테스트 그린 + ArchUnit 경계 강제 활성 + 데모 시나리오(정상·실패·타임아웃·동시성) 재현 가능

---

## 확장 학습 과제 (16일 이후 · 선택)

> 설계가 "의식적으로 미룬 것". 겪은 문제를 새 ADR로 남길 여지.

- [ ] 오케스트레이션 재구현 → 코레오그래피와 비교(ADR-0001)
- [ ] 단계별 타임아웃 / 워크플로 엔진(Temporal)(ADR-0003 §7)
- [ ] 셀러·운영자 페르소나, 재고 예약 모델(product-spec §3, PV-1)
- [ ] Schema Registry / `schemaVersion`(ADR-0007 §7)

---

## 정책 커버리지 (완료 시 전부 ✅)

| 학습 목표 | 관련 정책 | 커버 Day |
|-----------|-----------|----------|
| Kafka / EDA | PI-4·5·6, PT-* | 6~10·13~14 |
| DDD (Aggregate·불변식) | §1, PS-*, PV-1 | 3·8·9 |
| 보상 트랜잭션 | PB-*, PC-3, PV-5 | 11·12·13 |
| 멱등성 | PI-*, PS-4, PB-2, PT-3 | 5·9·10·11 |
| 동시성 제어 | PV-4, PC-4 | 8·13·15 |

---

*근거는 각 항목의 정책 ID(`PI-5`)·ADR 번호로 [`docs/`](./docs) 검색. 설계 서사는 [`docs/design.md`](./docs/design.md).*
