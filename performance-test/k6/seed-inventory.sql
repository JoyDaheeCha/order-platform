-- k6 부하테스트용 재고 시드 데이터
-- 사용법: docker-compose로 mysql 기동 후
--   mysql -h 127.0.0.1 -P 13306 -uroot -p<비밀번호> inventory_schema < seed-inventory.sql
USE inventory_schema;

INSERT INTO inventory (product_code, stock, reserved_stock, created_at, updated_at)
VALUES
    ('PERF-TEST-0001', 1000000, 0, NOW(6), NOW(6)),
    ('PERF-TEST-0002', 1000000, 0, NOW(6), NOW(6)),
    ('PERF-TEST-0003', 1000000, 0, NOW(6), NOW(6)),
    ('PERF-TEST-0004', 1000000, 0, NOW(6), NOW(6)),
    ('PERF-TEST-0005', 1000000, 0, NOW(6), NOW(6))
ON DUPLICATE KEY UPDATE
    stock = VALUES(stock),
    reserved_stock = 0,
    updated_at = NOW(6);
