package com.flab.orderplatform.inventory.application.annotation;

import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.*;

/**
 * Inventory 컨텍스트 전용 트랜잭션 애노테이션.
 * <p>
 * 컨텍스트별 TransactionManager 가 3개(order/payment/inventory)이므로
 * inventory 하위 코드는 {@code @Transactional} 대신 이 애노테이션을 사용한다.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Transactional(transactionManager = "inventoryTransactionManager")
public @interface InventoryTransactional {
}
