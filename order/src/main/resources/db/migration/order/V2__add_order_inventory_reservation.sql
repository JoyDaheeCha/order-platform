CREATE TABLE IF NOT EXISTS order_inventory_reservation
(
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    order_number VARCHAR(36) NOT NULL COMMENT '주문번호 (대외 노출용 비즈니스 키)',
    reserved_at  DATETIME(6) NOT NULL COMMENT '재고 선점 일시',
    is_released  TINYINT     NOT NULL DEFAULT 0 COMMENT '재고 선점 해제 여부',
    created_at   DATETIME(6) NOT NULL COMMENT '생성일',
    updated_at   DATETIME(6) NOT NULL COMMENT '수정일',
    created_by   VARCHAR(20) NOT NULL COMMENT '생성자',
    updated_by   VARCHAR(20) NOT NULL COMMENT '수정자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_inventory_reservation_order_number (order_number)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT '주문별 재고 선점 상태';

create index idx_order_order_inventory_reservation_id
    on orders (order_inventory_reservation_id);