-- 03-payments.sql — payment 스키마의 payments 테이블 (스캐폴드).
USE
payment_schema;

-- 결제
CREATE TABLE IF NOT EXISTS payments
(
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    order_number   VARCHAR(36) NOT NULL COMMENT '주문번호 (대외 노출용 비즈니스 키)',
    buyer_id       BIGINT      NOT NULL COMMENT '구매자 id',
    amount         BIGINT      NOT NULL COMMENT '총 결제액',
    status         VARCHAR(10) NOT NULL COMMENT '결제 상태 REQUESTED/COMPLETED/FAILED/REFUNDED',
    failure_reason VARCHAR(50) NULL COMMENT '결제 실패 사유',
    created_at     DATETIME(6)    NOT NULL COMMENT '생성일',
    updated_at     DATETIME(6)    NOT NULL COMMENT '수정일',
    PRIMARY KEY (id),
    UNIQUE KEY uk_payments_order_number (order_number)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT '결제';

-- 인박스 패턴 구현용 테이블
CREATE TABLE IF NOT EXISTS inbox
(
    id       BIGINT NOT NULL AUTO_INCREMENT,
    event_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '이벤트 UUID',
    aggregate_type VARCHAR(30) NOT NULL COMMENT '에그리거트명 (예. payment)',
    event_type VARCHAR(30) NOT NULL COMMENT '이벤트타입',
    payload LONGTEXT NOT NULL COMMENT '이벤트 페이로드', -- 추후 타 도메인에서 메시지 받을때 규격 안 맞더라도 일단 저장은 가능해야하므로 longtext로 저장
    status VARCHAR(10) NOT NULL COMMENT '이벤트 상태 (CREATED/PROCESSED/FAILED)',
    created_at     DATETIME(6)    NOT NULL COMMENT '생성일',
    updated_at     DATETIME(6)    NOT NULL COMMENT '수정일',
    PRIMARY KEY (id),
    UNIQUE KEY uk_inbox_event_id (event_id)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT '인박스';