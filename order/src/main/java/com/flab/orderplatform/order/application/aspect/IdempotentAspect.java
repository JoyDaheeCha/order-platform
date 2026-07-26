package com.flab.orderplatform.order.application.aspect;

import com.flab.orderplatform.order.application.annotation.Idempotent;
import com.flab.orderplatform.order.application.command.IdempotentKeyCommand;
import com.flab.orderplatform.order.common.exception.DuplicatedRequestException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 로직에서 key에 대해 중복 호출시 거부한다.
 *  - TTL(기본 1분) 내에 들어온 동일한 idempotentKey는 중복 요청으로 간주
 * 참고) {@link org.springframework.retry.annotation.Retryable} 보다 나중에 사용하기 위해 (Ordered.LOWEST_PRECEDENCE-2) 적용
 */
@Order(Ordered.LOWEST_PRECEDENCE-2)
@Aspect
@Component
public class IdempotentAspect {

    private static final String KEY_FORMAT = "idempotent:%s";

    private final RedissonClient redissonClient;
    /** 멱등키 보관 기간. 재배포 없이 조정할 수 있도록 설정값으로 둔다. */
    private final Duration ttl;

    public IdempotentAspect(RedissonClient redissonClient,
                            @Value("${idempotent.ttl:1m}") Duration ttl) {
        this.redissonClient = redissonClient;
        this.ttl = ttl;
    }

    @Around("execution(* com.flab.orderplatform.order.application..*(..)) && @annotation(idempotent)")
    public Object checkIdempotentKey(ProceedingJoinPoint jointPoint, Idempotent idempotent) throws Throwable {
        // redis 에 값 있으면 해당 값 읽고 현재 요청은 fail
        var key = KEY_FORMAT.formatted(findIdempotentKey(jointPoint));
        var bucket = redissonClient.getBucket(key);

        // 값이 없으면 set
        if (!bucket.setIfAbsent(IdempotentEntry.inProgress(), ttl)) {

            var existing = (IdempotentEntry) bucket.get();
            // 먼저 온 요청이 있는데, 먼저 온 요청의 응답값을 조회할 수 없거나(TTL만료로 delete), 아직 진행중이라면 중복으로 간주.
            if (existing == null || !existing.isCompleted()) {
                throw new DuplicatedRequestException(key);
            }
            return existing.payload(); // 앞선 요청이 성공했다면 캐싱된 값 반환
        }
        try {
            var result = jointPoint.proceed();
            bucket.set(IdempotentEntry.completed(result), ttl);
            return result;
        } catch (Throwable e) {
            bucket.delete(); // 실패한 요청은 재시도 가능하도록 키를 회수한다
            throw e;
        }
    }

    private String findIdempotentKey(ProceedingJoinPoint jointPoint) {
        for (var arg : jointPoint.getArgs()) {
            if (arg instanceof IdempotentKeyCommand command) {
                return command.getIdempotentKey();
            }
        }
        throw new IllegalStateException(String.format("Idempotent 메서드에 IdempotentKeyCommand 타입 인자가 없습니다. (method : %s)", jointPoint.getSignature()));
    }
}
