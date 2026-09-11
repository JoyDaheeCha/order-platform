package com.flab.orderplatform.shared.lock;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 분산락 애노테이션
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {
    /** 컨텍스트 네임스페이스 (예. inventory) */
    String prefix();
    /** 분산락 획득용 키 */
    String key();
    /** 다른 스레드에 의해 점유된 락을 기다릴 수 있는 최대 시간 */
    long waitTime() default 5;
    /** waitTime 단위 */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    /** 분산락 획득 실패시 실행되는 fallback 메서드의 이름. 대상 메서드와 동일한 파라메터의 public 메서드여야 한다. */
    String fallback();
}
