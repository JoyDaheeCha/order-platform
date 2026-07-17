-- 02-orders.sql — order 스키마의 orders 테이블 (스캐폴드).
--   Day 4 에서 Flyway 의 V1__create_orders.sql 이 이 테이블의 주인을 넘겨받으면 이 파일은 삭제한다.
--   그때 로컬 DB 는 `docker compose down -v` 로 초기화해야 한다 — init 스크립트는 최초 부팅에만
--   돌아서 파일만 지우면 기존 컨테이너엔 테이블이 남고, validate 는 여분 테이블을 탓하지 않아
--   조용히 남는다.
--
-- 왜 앱이 아니라 여기서 만드나:
--   ddl-auto 를 validate 로 유지하기 때문이다(D1~D16 불변). create 로 두면 Hibernate 가
--   자기가 만든 테이블을 자기가 읽어 네이밍이 틀려도 조용히 통과하고, 함정이 Day 4(Flyway 가
--   snake_case SQL 을 들고 오는 날)로 미뤄진다. 테이블을 "남"이 만들어야 validate 가 진짜
--   대조를 하고, 어긋나면 오늘 즉시 부팅이 깨진다.
--
--   total_amount·ordered_at 는 OrderEntity.totalAmount·orderedAt 의 snake_case 변환 결과다 —
--   네이밍 전략이 빠지면 Hibernate 는 `totalAmount` 컬럼을 찾다가 실패한다.
--
-- 컬럼이 이게 전부인 이유: Order Aggregate 는 Day 3 에 설계된다. 여기 있는 건 배관을 증명할
--   최소 집합이고, 주문 라인(PO-1~3)·상태 전이는 도메인이 정한 뒤에 온다. (OrderEntity 참고)

USE order_schema;

CREATE TABLE IF NOT EXISTS orders (
    id           VARCHAR(36)  NOT NULL,
    total_amount BIGINT       NOT NULL,
    ordered_at   DATETIME(6)  NULL,
    status       VARCHAR(20)  NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
