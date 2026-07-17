# Day 2 — 멀티 DataSource: 컨텍스트 경계를 *물리*로 강제

> **이 문서의 역할**: [`todo.md`](../../todo.md)가 "무엇을 할 것인가"라면, 이 문서는 **"실제로 무슨 일이 있었나"**다.
> 계획이 왜 바뀌었고, 무엇에 막혔고, 에러 메시지가 어떻게 거짓말을 했는지를 남긴다.
> 설계 결정은 ADR이, 실행 계획은 todo.md가, **판단의 흔적은 여기가** 갖는다.

- **날짜**: 2026-07-17
- **커밋**: `5239c83` → `ac26255` → `27cf0fe` (17 files, +652 / −57)
- **결과**: ✅ 완료 기준 ①② 모두 그린. 전체 **8개 테스트가 `docker compose up` 없이** 통과.

---

## 1. 결과부터

| 완료 기준 | 결과 |
|---|---|
| ① 더미 엔티티가 `order` 스키마에 **실제로 저장**된다 | PASS |
| ② **`payment`의 EntityManager Metamodel에 order 엔티티가 없다** | PASS |
| (부수) ArchUnit 경계 5 + 스모크 1 | PASS |

②가 오늘의 본체다. ADR-0004가 B-1(단일 DataSource + `@Table(schema=)`)을 *"경계가 리뷰 의존"*이라는 이유로 기각하고 B-2를 고른 근거가, 이제 **실행 가능한 증거**로 레포에 남는다. 면접에서 꺼낼 수 있는 건 ADR의 문장이 아니라 이 테스트다.

---

## 2. 계획이 세 번 바뀌었다 (오늘 가장 값진 부분)

착수 전 계획은 "이벤트 계약 10종 + 멀티 DataSource + Flyway + Testcontainers"였다. 실제로 한 건 **멀티 DataSource 하나**다. 셋 다 *같은 원리*로 잘려나갔다 — **사용처가 없는 것은 지금 만들지 않는다.**

### 2-1. 이벤트 계약 10종 → Day 6

실무에서 `shared` 계약을 미리 못 박는 이유는 **다른 팀이 그 계약에 맞춰 동시에 코딩해야** 하기 때문이다. 이 프로젝트는 한 사람이 D6→D7→D8→D11 순서로 **직렬로** 간다. 아무도 안 막혀 있으니 미리 확정해서 얻는 게 없다.

반면 잃는 건 있다. `StockShortage`는 Day 11에야 첫 소비자가 생기는데, 그때 `shortageProductIds`만으로 부족하다는 걸 발견하면 **두 번 쓴다.** 계약 설계 자체는 ADR-0007에서 이미 끝났고, 코드로 옮기는 건 사용처가 생긴 만큼만 하면 된다.

> 근거의 징후는 계획 안에 이미 있었다. `Money` vs `long`, `eventType` 매핑 방식 — 둘 다 **실제 발행자가 있어야 판단되는** 문제인데 사용처 없이 결정하려 했다. 그건 설계가 아니라 추측이다.

### 2-2. Flyway → Day 4

처음엔 "실무 표준이니 Flyway"라고 했는데 근거가 **순환논리**였다.

> `ddl-auto: validate`니까 → validate는 테이블을 안 만드니까 → 만들 주체가 필요 → Flyway

`validate`는 제약이 아니라 **Day 1에 우리가 고른 선택**이다. 고른 전제로 결론을 정당화한 것.

Flyway의 가치는 *"같은 스키마 변경을 여러 환경에 · 시간에 걸쳐 · 결정적으로 반복 적용"*이다. Day 2 시점에 **운영 DB 없음 / 다른 개발자 없음 / 배포 없음** — 그 문제의 **인스턴스가 0개**였다. 게다가 첫 작품이 "Day 4에 지울 더미 테이블의 생성 기록"이고, Flyway 규칙상 `V1`은 못 지워서 `V2__drop_ping.sql`까지 따라온다. 의식(ceremony)이지 가치가 아니다.

**미루는 비용도 0이었다.** 보통 "나중에 Flyway 붙이기"가 싫은 건 기존 스키마에 `baseline`을 떠야 해서인데, Day 4 시점 스키마는 그때 새로 만드는 `orders` 하나뿐이다. 레트로핏이 아니라 **시작일이 이틀 늦는 것**뿐.

### 2-3. `ddl-auto: create` → `validate` 유지

`create`를 쓰자던 제안엔 부작용이 있었다. **네이밍 전략 함정이 Day 2에 숨는다.** `create`는 자기가 만든 테이블을 자기가 읽으니 camelCase든 뭐든 자기들끼리 정합해서 통과하고, Day 4에 Flyway가 snake_case SQL을 들고 와야 터진다.

`validate`를 유지하면 **테이블을 남이 만들고 Hibernate가 대조**하니 어긋나면 즉시 부팅이 깨진다. 함정을 미루는 게 아니라 **가장 싼 순간(더미 테이블 하나)에 앞당겨 터뜨린다.** 덤으로 설정이 D1~D16 내내 안 흔들린다.

> 아래 3-2에서 이 판단이 실제로 값을 했다.

---

## 3. 무엇을 만들었나

```
order|payment|inventory / infrastructure/persistence/
  └── *PersistenceConfig.java     DataSource · EMF · TransactionManager · @EnableJpaRepositories

order / infrastructure/persistence/
  ├── PingEntity.java             배관 점검용 더미 (Day 4 삭제)
  └── PingRepository.java

bootstrap/
  ├── OrderPlatformApplication    자동설정 2개 exclude
  └── test/persistence/
      ├── MySqlTestContainer      단일 MySQL + docker/mysql/init 마운트
      └── ContextIsolationTest    완료 기준 ①②

docker/mysql/init/02-ping.sql     더미 테이블 (Day 4 삭제)
```

### 핵심 결정 세 가지

**자동설정 2개(`DataSource`·`HibernateJpa`)를 껐다.** 둘 다 "DataSource는 하나"를 전제한다. 3개면 `@Primary` 하나만 배선하고 나머지를 방치해 **반쯤 동작하는** 상태를 만드는데, 그게 디버깅하기 제일 고약하다. 껐더니 `spring.datasource.*`·`spring.jpa.*`가 **죽은 설정**이 돼서 제거하고, 각 컨텍스트가 `datasource.{context}.*`를 읽고 Hibernate 속성을 EMF에 직접 주입한다.

**`@Primary`를 일부러 안 붙였다.** 붙이면 한정자 없는 `@Transactional`이 **조용히** 그 하나를 쓴다. order 리포지토리가 payment의 TransactionManager 아래서 도는데도 예외 없이 *"save는 성공했는데 DB엔 없는"* 상태가 된다(EntityManager가 그 트랜잭션에 join되지 않아 flush·commit이 안 일어남). 후보가 셋이면 Spring이 **시끄럽게** 실패한다. 완료 기준 ①이 저장 후 **다시 읽는** 이유도 이것이다 — 부팅 성공도, Metamodel 단언도, actuator health(`SELECT 1`)도 이걸 못 잡는다.

**`PingEntity`는 의도적으로 무의미하다.** 진짜 `OrderEntity`를 앞당겨 쓰자는 안을 기각했는데, Order Aggregate가 **Day 3**에 설계되기 때문이다. 영속화 모델을 도메인보다 먼저 쓰면 Day 3의 도메인이 **이미 있는 테이블에 끌려간다** — DDD가 싸우는 DB-first 설계고, Day 3 자문 ③("도메인이 JPA에 의존하면 뭐가 나빠지나")을 형식만 남긴다. 게다가 TM 배선을 증명하는 능력은 `PingEntity`나 `OrderEntity`나 **완전히 동일**하다(TM은 엔티티가 뭔지 신경 쓰지 않는다). 검증 가치는 0만큼 늘고 위험만 붙는다.

---

## 4. 겪은 것 — 에러 메시지가 원인을 안 가리킨 사례들

### 4-1. `Could not find a valid Docker environment` (진짜 원인: 버전 비호환)

Testcontainers가 Docker를 못 찾는다고 했다. **그런데 소켓은 멀쩡했다.**

```
$ curl --unix-socket /var/run/docker.sock http://localhost/info   → HTTP 200
```

400 응답 본문이 *"필드는 다 있는데 값이 빈 스텁"*인 게 단서였다. 서버 API 범위를 재보니:

```
ApiVersion: 1.55 | MinAPIVersion: 1.40
v1.38/info → 400   v1.39/info → 400   ← 응답 본문이 Testcontainers가 받은 것과 동일
v1.40/info → 200   v1.44/info → 200
```

**확정.** Boot 3.4.1이 핀한 Testcontainers 1.20.4(2024-12)의 docker-java 3.4.0이 **API 1.40 미만**을 요청하는데, 로컬 Docker Engine **29.6.1**은 `MinAPIVersion=1.40`이라 거절한 것. Testcontainers는 그 400을 "Docker 없음"으로 오인했다.

→ **1.21.4로 올려 해결**(같은 메이저라 API 호환). BOM을 의도적으로 덮어썼다.

> 배운 것: **BOM의 "검증된 조합"도 런타임 환경이 라이브러리보다 새로우면 깨진다.** BOM은 라이브러리끼리의 호환을 보장하지, 내 Docker 엔진과의 호환은 모른다.
>
> 반성: 중간에 `mysql.init.dir` 프로퍼티가 원인이라고 **추측했다가 틀렸다.** 로그를 열어보니 그 프로퍼티는 정상 전달되고 있었다. 추측 대신 로그를 먼저 봤어야 했다.

### 4-2. 네이밍 전략은 진짜 load-bearing인가 (변이 테스트)

테스트가 통과한다고 **의미 있게** 통과한 건 아니다. `PingEntity.noteText`를 camelCase로 둔 이유가 "전략이 빠지면 오늘 터진다"였으니, 일부러 깨봤다.

```
physical_naming_strategy 제거 → SchemaManagementException (AbstractSchemaValidator) → 부팅 실패
원복                          → 통과
```

**예측대로 오늘 터졌다.** 2-3의 판단(`create` 대신 `validate`)이 실물로 값을 한 지점이다. `create`였다면 조용히 통과하고 Day 4로 미뤄졌을 것이다.

### 4-3. `Schema-validation: missing table [ping]` (스모크 테스트)

Day 1의 스모크 테스트가 깨졌다. **MySQL init 스크립트는 데이터 디렉토리가 빈 최초 부팅에만 실행된다** — 이미 떠 있던 compose 컨테이너엔 새로 만든 `02-ping.sql`이 반영될 리가 없었다.

뿌리는 **테스트가 외부 컨테이너 상태에 의존**한다는 것. Testcontainers를 고른 이유가 바로 그거였는데 한 테스트만 예외로 남아 있었다. 그 테스트도 자립시켰다 → **이제 전체 테스트가 `docker compose up` 없이 돈다.**

### 4-4. 계획 문서가 틀렸던 것 (착수 전 검증으로 발견)

- **`SpringPhysicalNamingStrategy`는 Boot 3에 없다.** `HibernateProperties$Naming` 바이트코드로 확인 → Hibernate의 `CamelCaseToUnderscoresNamingStrategy`로 대체됐다. Boot 2 기억으로 그대로 썼으면 컴파일부터 실패.
- **Boot는 `dialect`를 기본 설정하지 않는다.** 같은 확인에서 발견. `spring.jpa.database-platform`이 없으면 JDBC 메타데이터 자동 감지에 맡긴다. 그래서 우리도 안 넣었다(서버 실제 버전을 읽어 버전별 SQL을 쓰므로 그편이 낫다).
- **`@ServiceConnection`을 못 쓴다.** `spring.datasource.*`에 값을 꽂아주는 물건인데, 그걸 읽는 주체가 자동설정이고 우리가 껐다. → `@DynamicPropertySource`로 직접 매핑.

---

## 5. 자문 — 오늘 나온 답

**① 스키마를 분리하면 경계가 왜 *물리적으로* 강제되나 (EntityManager에 안 보인다는 게 무슨 뜻)**

EMF는 부팅 시 `packagesToScan`을 뒤져 `@Entity`를 모으고, 그 결과가 **Metamodel** — EMF가 세상에 존재한다고 믿는 엔티티의 전부다. payment의 EMF가 `OrderEntity`를 못 찾는 건 **"권한이 없다"가 아니라 그 클래스의 존재 자체를 모르는 것**이다. 매핑이 없으니 SQL을 만들 방법이 없고, 물어볼 수조차 없다.

**② ADR-0004가 B-1을 기각한 이유 — "규약"과 "구조"의 차이가 실제로 뭘 바꾸나**

B-1은 **DataSource 1개 = EMF 1개 = 하나의 Metamodel에 전 엔티티**다. 어차피 한 EMF가 다 알고 있으니 네이티브 쿼리로 cross-schema JOIN이 **물리적으로 가능**하고, 경계는 리뷰가 지킬 뿐이다. B-2가 하는 일은 **그 하나의 Metamodel을 셋으로 쪼개는 것**. 잘못된 코드가 "금지"되는 게 아니라 **작동을 안 한다.**

**③ 자동설정을 끄면 원래 자동으로 되던 것 중 무엇이 사라지나**

오늘 세 번 청구됐다. (1) `spring.datasource.*`·`spring.jpa.*` 바인딩 — 죽은 설정이 됨. (2) Hibernate 기본 속성 — **네이밍 전략이 사라져 4-2처럼 부팅이 깨진다.** (3) OSIV 인터셉터 — 등록되지 않음(우리가 원하던 바). 그리고 (4) **생태계의 편의까지 같이 꺼진다** — `@ServiceConnection`이 무력해진 게 그 사례다. 자동설정만 꺼지는 게 아니라 **그 위에 얹혀 있던 것들이 함께 꺼진다.**

**📚 멘토님 질문 — 하나의 트랜잭션이 multi datasource를 거쳐야 한다면?**

이 프로젝트의 답은 **"그 상황을 만들지 않는다"**(ADR-0004 B-2 = 컨텍스트 간 단일 트랜잭션 금지, Saga로 정합). 다만 그 앞단까지 답할 수 있어야 한다 — XA/2PC는 왜 존재하고 왜 실무에서 기피되나(블로킹·코디네이터 SPOF), `ChainedTransactionManager`는 왜 원자성을 못 주는 반쪽인가(그래서 deprecated), 그래서 왜 Saga로 가나. *(→ 별도 정리 필요)*

---

## 6. 남은 것

- [ ] **로컬 `bootRun`은 아직 깨져 있다** — compose의 MySQL에 `ping` 테이블이 없다. `docker compose down -v && docker compose up -d` 필요(`-v`가 볼륨을 지우므로 직접 확인 후 실행). 테스트는 이것과 무관하게 통과한다.
- [ ] 멘토님 질문(XA/2PC → `ChainedTransactionManager` → Saga) 상세 정리
- [ ] **ADR 후보**: 검증=Testcontainers / DDL=Flyway(D4부터) 는 옵션 비교와 트레이드오프가 있었는데 ADR이 없다. 다만 **Flyway를 실제로 붙여본 Day 4 이후에 쓰는 게 맞다** — 트레이드오프를 겪기 전에 쓰면 근거가 아니라 추측이 된다(2-1과 같은 이유).

## 7. Day 3 예고

Order Aggregate(순수 자바). 오늘 `PingEntity`를 가짜로 둔 값을 받는 날 — **도메인이 테이블을 모르는 상태에서** 설계할 수 있다.
