package com.flab.orderplatform.order.application.aspect;

import com.flab.orderplatform.order.application.annotation.Idempotent;
import com.flab.orderplatform.order.application.exception.DuplicatedRequestException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 로직에서 key에 대해 중복 호출시 거부한다.
 */
@Aspect
@Component
public class IdempotentAspect {

    RedissonClient redissonClient;

    @Around("execution(* com.flab.orderplatform.order.application..*(..)) && @annotation(idempotent)")
    public Object checkIdempotentKey(ProceedingJoinPoint jointPoint, Idempotent idempotent) throws Throwable {
        // redis 에 값 있으면 해당 값 읽고 현재 요청은 fail
        var key = "idempotent:" + idempotent.key();
        var bucket = redissonClient.getBucket(key);

        // 값이 없으면 set 하고 있으면 exception 처리
        if (!bucket.setIfAbsent("IN_PROGRESS", Duration.ofMinutes(10))) {
            throw new DuplicatedRequestException(key);
        }
        try {
            return jointPoint.proceed();
        } catch (Throwable e) {
            bucket.delete(); // 실패한 요청은 재시도 가능하도록 키를 회수한다
            throw e;
        }
    }
}
