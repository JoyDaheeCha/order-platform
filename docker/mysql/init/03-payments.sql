-- 03-payments.sql — payment 스키마의 payments 테이블 (스캐폴드).
USE
payment_schema;

-- 결제
CREATE TABLE IF NOT EXISTS payments
(
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    order_id       VARCHAR(30) NOT NULL COMMENT 'order 컨텍스트 상관관계 키',
    buyer_id       BIGINT      NOT NULL,
    amount         BIGINT      NOT NULL COMMENT 'KRW 정수. 1회·전액 (PP-2)',
    status         VARCHAR(10) NOT NULL COMMENT 'REQUESTED/COMPLETED/FAILED/REFUNDED',
    failure_reason VARCHAR(50) NULL COMMENT '결제 실패 사유',
    created_at     DATETIME    NOT NULL COMMENT '생성일',
    updated_at     DATETIME    NOT NULL COMMENT '수정일',
    PRIMARY KEY (id)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT '결제';

-- 인박스 패턴 구현용 테이블
CREATE TABLE IF NOT EXISTS inbox
(
    id       BIGINT NOT NULL AUTO_INCREMENT,
    event_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '이벤트 UUID',
    payload LONGTEXT NOT NULL COMMENT '이벤트 페이로드', -- 추후 타 도메인에서 메시지 받을때 규격 안 맞더라도 일단 저장은 가능해야하므로 longtext로 저장
    status VARCHAR(10) NOT NULL COMMENT '이벤트 상태 (CREATED/PROCESSED/FAILED)',
    created_at     DATETIME    NOT NULL COMMENT '생성일',
    updated_at     DATETIME    NOT NULL COMMENT '수정일',
    PRIMARY KEY (id)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT '인박스';