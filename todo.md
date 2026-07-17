# todo — order-platform 구현 일정 (하루 8h 기준)

> **이 문서의 역할**: 설계 문서(product-spec·policy·architecture·design·ADR 7건)는 전부 Accepted다.
> 이 문서는 그걸 코드로 옮기는 **하루 단위 실행 계획**이다. 각 Day는 끝나면 **"돌아가는 것"** 하나가
> 남도록 잘랐다 — 리뷰어가 매일의 진척을 실제로 확인할 수 있게.
>
> 총 **약 16 영업일(≈3.5주)**. 근거 정책은 `PI-5`·`PV-4` 같은 ID로, 결정은 ADR 번호로 표기.
> 현재 좌표: 설계 100% / 코드 골격만(도메인 로직 0줄) — [`docs/onboarding.md`](./docs/onboarding.md).

---

## 산정 기준 (리뷰어용 메모)

- **하루 8h = 구현 + 학습 + 검증** 포함. 학습 프로젝트라 "왜 이렇게 되는가" 확인 시간을 넣었다.
- 각 Day는 **독립적으로 데모 가능한 결과물**로 끝난다. 빅뱅 통합을 만들지 않는다.
- 순서 원칙: **가장 얇은 주문 한 건을 끝까지 관통(walking skeleton)** → 살 붙이기. (`onboarding.md §3`)
- 리스크가 큰 날(멀티 DataSource, Outbox 릴레이, 동시성)은 별도 표시(⚠️)하고 여유를 뒀다.
- **각 Day에 `📚 완료 후 자문`** = 그날 작업을 마치고 스스로에게 던지는 질문. **막힘없이 답할 수 있으면 그 개념을 체득한 것**이다(면접 회고에도 그대로 쓴다). 이 프로젝트의 산출물 절반은 코드가 아니라 이 답들이다(design.md §0).

> 💡 **학습 흐름 한눈에**: Kafka 기초(D1) → 컨텍스트 경계·스키마 분리(D2) → DDD 도메인(D3~4) → 멱등성 생산자측(D5) → **이벤트 계약·Outbox/dual-write(D6)** → 코레오그래피 구독(D7~8) → **Inbox/effectively-once(D9)** → **Saga 보상(D11~12)** → 코레오그래피의 빚: 타임아웃(D13)·재시도/DLQ(D14) → **동시성 4기법 비교(D15)** → 아키텍처 검증(D16)

| 주차 | 산출물 |
|------|--------|
| **Week 1** (Day 1~5) | 무대 세팅 + Order 도메인/저장/API 골격 |
| **Week 2** (Day 6~10) | 첫 관통 완성 → Saga happy path (3 컨텍스트 연결) |
| **Week 3** (Day 11~16) | 보상 트랜잭션 · 타임아웃 · 동시성 · 검증 종합 |

---

# Week 1 — 무대 세팅 + Order 골격

### Day 1 — 로컬 인프라 + 빌드 그린 `Phase 0`
**목표**: 앱이 뜨고, MySQL·Kafka·Redis에 붙는다.
> 📚 **완료 후 자문**: ① Kafka의 토픽·파티션·컨슈머그룹은 각각 무엇을 결정하나? ② KRaft 모드는 주키퍼가 하던 어떤 역할을 대체했고, 왜 없애는 방향으로 갔나?
- [x] `docker-compose.yml` — MySQL 8 · Kafka(KRaft 단일 노드) · Redis (design.md §5: DB는 스키마로만 격리)
- [x] 5개 모듈 `build.gradle`에 필요한 starter 채우기 (libs.versions.toml 참조)
- [x] `application.yml` 기본 골격 + `local` 프로파일
- [x] `OrderPlatformApplication` 부팅 확인
- **✅ 완료 기준**: `docker compose up` + `./gradlew bootRun` 성공, 컨테이너 3개 연결 로그 확인

### Day 2 — 멀티 DataSource: 컨텍스트 경계를 *물리*로 강제 ⚠️ `Phase 0`
**목표**: 컨텍스트마다 자기 스키마만 보는 DataSource가 서고, "타 스키마가 안 보인다"를 테스트로 증명한다.
> ✅ **완료** — 실제로 무슨 일이 있었나(계획 변경 3건·겪은 함정·자문 답변): [`docs/worklog/day-02.md`](./docs/worklog/day-02.md)
> 📚 **완료 후 자문**:
> ① 스키마를 분리하면 컨텍스트 경계가 왜 *물리적으로* 강제되나(EntityManager에 안 보인다는 게 무슨 뜻)?
> ② ADR-0004가 B-1(단일 DataSource + `@Table(schema=)`)을 기각한 이유는 — "규약으로 막는 것"과 "구조로 막는 것"의 차이가 실제로 무엇을 바꾸나?
> ③ Spring Boot 자동설정을 끄고 DataSource·EMF·TransactionManager를 손으로 배선하면, 원래 자동으로 되던 것 중 무엇이 사라지나?
> > 📚 **멘토님 질문**:
> ① 하나의 트랜잭션이 multi datasource를 거쳐야 한다면 어떻게 처리?
> > → 이 프로젝트의 답은 **"그 상황을 만들지 않는다"**(ADR-0004 B-2, 컨텍스트 간 단일 트랜잭션 금지). 다만 그 앞단까지 답할 수 있어야 한다 — XA/2PC는 왜 존재하고 왜 실무에서 기피되나(블로킹·코디네이터 SPOF), `ChainedTransactionManager`는 왜 원자성을 못 주는 반쪽인가(그래서 deprecated), 그래서 왜 Saga로 가나.
- [x] `IntegrationEvent` **제거** (ADR-0007이 기각한 옵션 b) — 참조처 0, 순수 삭제
- [x] **DataSource 3세트**(ADR-0004 B-2) — 컨텍스트별 DataSource·EntityManagerFactory·TransactionManager + `@EnableJpaRepositories`
- [x] **자동설정 2개 제외** — `DataSource`·`HibernateJpa` AutoConfiguration. DataSource가 3개면 자동설정이 `@Primary` 하나만 배선하고 나머지를 방치해 "반쯤 동작하는" 상태를 만든다 → 전부 수동 배선해 미스터리를 0으로
- [x] **Hibernate 속성 직접 주입** — 수동 EMF는 Boot가 넣어주던 걸 **하나도** 못 받는다(**`spring.jpa.*`는 수동 EMF에 적용되지 않음**). `HibernateProperties$Naming` 바이트코드로 확인한 Boot 3.4.1 실제 기본값:
  - `hibernate.hbm2ddl.auto` = `validate`
  - `hibernate.physical_naming_strategy` = `org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy` ⚠️ **Boot 3에서 `SpringPhysicalNamingStrategy`는 사라졌다** — Hibernate 자체 클래스로 대체됨(Boot 2 기억으로 쓰면 컴파일 실패)
  - `hibernate.implicit_naming_strategy` = `org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy` (이쪽은 Spring 것이 유지)
  - `hibernate.dialect` = **설정하지 않음**. Boot 도 `spring.jpa.database-platform` 이 없으면 안 넣고 Hibernate 의 JDBC 메타데이터 자동 감지에 맡긴다(위 바이트코드가 적용하는 건 네이밍 전략 2개뿐). 서버 실제 버전을 읽어 버전별 SQL 을 쓰므로 그편이 낫다
- [x] **`application.yml`의 `spring.jpa.hibernate.ddl-auto` 정리** — 자동설정을 끄면 이 줄은 **죽은 설정**이 된다(살아 있는 척하며 아무 일도 안 함). `validate`라는 *정책*은 그대로지만 *선언 위치*가 수동 EMF 속성으로 옮겨간다
- [x] 더미 `PingEntity` + `docker/mysql/init/02-ping.sql` — **`validate` 정책은 D1~D16 내내 유지**. Day 4에서 `OrderEntity`·Flyway로 대체하며 삭제
- [x] **Testcontainers MySQL** — `docker/mysql/init` 디렉토리를 컨테이너 `/docker-entrypoint-initdb.d/`에 마운트해 **docker-compose와 스키마 초기화 출처를 하나로**. `@DynamicPropertySource`로 컨테이너 URL을 `datasource.*` 3개에 매핑
- **✅ 완료 기준**: ① 더미 엔티티가 `order` 스키마에 저장됨 ② **`payment`의 EntityManager Metamodel에 order 엔티티가 없음**을 테스트로 단언 ← ADR-0004가 B-2를 고른 근거 그 자체
> ⚠️ 하루 리스크의 대부분은 수동 EMF 배선. **대표 함정**: 자동설정을 끄면 Boot가 넣어주던 네이밍 전략이 사라져, SQL이 만든 `some_field`와 Hibernate가 기대하는 `someField`가 어긋나 `validate`가 부팅을 깬다(로그가 원인을 안 알려줌).
> 📌 **`ddl-auto: create`를 쓰지 않는 이유** — `create`는 자기가 만든 테이블을 자기가 읽으니 네이밍이 틀려도 자기들끼리 정합해 **조용히 통과**하고, 함정을 Day 4(Flyway가 snake_case SQL을 들고 오는 날)로 미룬다. `validate` + 외부가 쓴 SQL이면 **오늘, 더미 테이블 하나 놓고 즉시** 터진다. 가장 싼 순간에 앞당겨 터뜨리는 쪽이 낫다. 설정도 안 흔들린다.
> 📌 완료 기준 ②는 **DataSource가 2세트 이상이어야 성립**한다(비교 대상이 있어야 "안 보인다"를 단언). 그래서 3세트는 "여유되면"이 아니라 필수다. 반대로 ②는 **테이블이 필요 없다** — Metamodel은 부팅 시점의 매핑 정보라 DB 상태와 무관하다. 테이블이 필요한 건 ①뿐.
> 📌 **결정 기록(ADR 없음)**: 검증은 Testcontainers, DDL은 Flyway — 단, **Flyway는 Day 4부터**. 오늘 넣으면 첫 작품이 "Day 4에 지울 더미 테이블의 생성 기록"이라 의식(ceremony)일 뿐이고, 가장 위험한 날에 움직이는 부품만 늘린다. 운영 DB도 다른 개발자도 배포도 없는 지금 Flyway가 푸는 문제는 **인스턴스가 0개**다. 미루는 비용도 0 — Day 4 시점 스키마는 새로 만드는 테이블 하나뿐이라 뜰 `baseline`이 없다. 그동안 더미 테이블 DDL은 이미 `01-schemas.sql`이 사는 `docker/mysql/init`에 얹는다(프로젝트의 진짜 DDL은 D4부터 전부 Flyway 소유).
> 📌 자문 ③의 답이 **1번(의존성)에서 이미 나온다**: `@ServiceConnection`(Testcontainers 표준 편의)을 못 쓴다. 그건 `spring.datasource.*`에 값을 꽂아주는데 그걸 읽는 주체가 자동설정이고, 우리가 그걸 껐기 때문이다. **자동설정만 꺼지는 게 아니라 그 위에 얹혀 있던 생태계의 편의까지 같이 꺼진다.**
> 📌 **이벤트 계약은 Day 6으로 이동.** 첫 사용처(Outbox 릴레이가 `EventEnvelope`로 포장해 발행)가 거기다. 한 사람이 직렬로 진행하는 프로젝트라 계약을 미리 못 박아 풀리는 병렬 작업이 없고, 발행자 없이 쓴 payload는 첫 소비자가 생길 때 어차피 고쳐 쓰게 된다.
>
> **— 실제로 겪은 것 (완료 후 기록) —**
> 🔥 **Testcontainers 1.20.4 → 1.21.4 로 BOM 을 덮어썼다.** 증상은 `Could not find a valid Docker environment` 였지만 소켓은 멀쩡했다(curl 로 `/info` 200). 진짜 원인: Boot 3.4.1 이 핀한 1.20.4 의 docker-java 3.4.0 이 **API 1.40 미만**을 요청하는데 로컬 Docker Engine 29 는 `MinAPIVersion=1.40` 이라 **400(빈 스텁)** 으로 거절 → Testcontainers 가 "Docker 없음"으로 오인. `curl /v1.39/info` 가 같은 400 본문을 뱉어 확정했다. **에러 메시지가 원인을 가리키지 않은 사례** — BOM 의 "검증된 조합"도 런타임 환경이 라이브러리보다 새로우면 깨진다.
> 🔥 **네이밍 전략이 진짜 load-bearing 인지 변이 테스트로 확인했다.** `physical_naming_strategy` 만 빼니 `SchemaManagementException`(AbstractSchemaValidator)으로 부팅 실패 → 원복하니 통과. 예측대로 **오늘** 터졌다(`create` 였다면 조용히 통과하고 Day 4 로 미뤄졌을 것).
> 🔥 **스모크 테스트가 `missing table [ping]` 으로 깨졌다** — MySQL init 스크립트는 최초 부팅에만 돌아서, 이미 떠 있던 compose 컨테이너엔 `02-ping.sql` 이 반영되지 않았다. 외부 컨테이너 의존이 원인이라 그 테스트도 Testcontainers 로 자립시켰다. **이제 전체 테스트가 `docker compose up` 없이 돈다.**

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
- [ ] JPA `OrderEntity` + repository 어댑터 (order 스키마)
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
- [ ] `onboarding.md` 갱신 — "골격만" → 실제 상태
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
