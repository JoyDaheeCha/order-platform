-- 02-ping.sql — 배관 점검용 더미 테이블. (todo.md Day 2)
--   Day 4 에서 OrderEntity·Flyway 가 오면 이 파일과 PingEntity 를 함께 삭제한다.
--
-- 왜 앱이 아니라 여기서 만드나:
--   ddl-auto 를 validate 로 유지하기 때문이다(D1~D16 불변). create 로 두면 Hibernate 가
--   자기가 만든 테이블을 자기가 읽어 네이밍이 틀려도 조용히 통과하고, 함정이 Day 4(Flyway 가
--   snake_case SQL 을 들고 오는 날)로 미뤄진다. 테이블을 "남"이 만들어야 validate 가 진짜
--   대조를 하고, 어긋나면 오늘 즉시 부팅이 깨진다.
--
--   note_text 는 PingEntity.noteText 의 snake_case 변환 결과다 — 네이밍 전략이 빠지면
--   Hibernate 는 `noteText` 컬럼을 찾다가 실패한다. 그게 이 파일의 존재 이유 절반이다.
--
-- 진짜 DDL(orders·payments·stocks·outbox…)은 Day 4 부터 전부 Flyway 소유다.

USE order_schema;

CREATE TABLE IF NOT EXISTS ping (
    id        VARCHAR(36)  NOT NULL,
    note_text VARCHAR(255) NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
