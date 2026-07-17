-- 01-schemas.sql — 컨텍스트별 스키마 생성. (ADR-0004 결정 A / A-2 채택)
--   컨텍스트 경계를 "규약"이 아니라 "물리"로 강제하기 위해서다

CREATE DATABASE IF NOT EXISTS order_schema
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

CREATE DATABASE IF NOT EXISTS payment_schema
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

CREATE DATABASE IF NOT EXISTS inventory_schema
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;
