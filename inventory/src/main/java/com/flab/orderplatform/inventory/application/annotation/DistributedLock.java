package com.flab.orderplatform.inventory.application.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 분산락 애노테이션
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {
    /** 분산락 획득용 키 */
    String key();
    /** 다른 스레드에 의해 점유된 락을 기다릴 수 있는 최대 시간 */
    long waitTime() default 5;
    /** waitTime 단위 */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    /** 분산락 획득 실패시 실행되는 fallback 메서드의 이름*/
    String fallback();
}
