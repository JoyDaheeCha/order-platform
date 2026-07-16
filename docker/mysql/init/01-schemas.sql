-- 01-schemas.sql — 컨텍스트별 스키마 생성. (ADR-0004 결정 A / A-2 채택)
--
-- 이 파일은 MySQL 컨테이너의 **데이터 디렉토리가 비어 있을 때 딱 한 번** 실행된다.
-- 이미 뜬 적이 있는 볼륨에는 반영되지 않으므로, 이 파일을 고쳤다면:
--     docker compose down -v && docker compose up -d
--
-- 왜 스키마를 나누나 (Day 2 자문 ③의 실물):
--   컨텍스트 경계를 "규약"이 아니라 "물리"로 강제하기 위해서다.
--   같은 스키마에 다 있으면 누군가 JOIN 한 줄로 경계를 뚫어도 컴파일도 테스트도 안 막는다.
--   스키마를 나누고 DataSource 를 나누면(ADR-0004 B-2), order 의 EntityManager 에는
--   payment/inventory 테이블이 **아예 존재하지 않는다** → JOIN을 쓰고 싶어도 쓸 수가 없다.
--
-- 테이블은 여기서 만들지 않는다. 스키마라는 "빈 그릇"만 준비하고,
-- 그 안의 테이블은 Day 2 이후 각 컨텍스트가 자기 것을 정의한다.

CREATE DATABASE IF NOT EXISTS order_schema
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

CREATE DATABASE IF NOT EXISTS payment_schema
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

CREATE DATABASE IF NOT EXISTS inventory_schema
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

-- 학습용 단순화: 세 스키마 모두 root 로 접근한다.
-- 경계를 DB 권한으로까지 강제하려면 컨텍스트별 계정을 만들고 자기 스키마만 GRANT 하면 된다
-- (order_user 는 payment_schema 에 SELECT 조차 못 하게). 지금은 DataSource 분리로 충분하다고 보고 미룬다.
