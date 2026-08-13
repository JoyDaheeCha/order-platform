package com.flab.orderplatform.inventory.application.aspect;

import com.flab.orderplatform.inventory.application.annotation.DistributedLock;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.RedissonMultiLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {
    private static final String KEY_FORMAT = "inventory:lock:%s";
    private final RedissonClient redissonClient;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("execution(* com.flab.orderplatform.inventory.application..*(..)) && @annotation(distributedLock)")
    public Object applyDistributedLock(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        var keys = convertToKey(joinPoint, distributedLock.key());
        var locks = keys.stream()
                .sorted()
                .map(key -> redissonClient.getLock(KEY_FORMAT.formatted(key)))
                .toArray(RLock[]::new);
        var multiLock = new RedissonMultiLock(locks);

        boolean isLockAcquired;
        try {
            // 락 만료는 watchDog에서 자동으로 관리
            isLockAcquired = multiLock.tryLock(distributedLock.waitTime(), distributedLock.timeUnit());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return invokeFallbackMethod(joinPoint, distributedLock);
        }

        if (!isLockAcquired) {
            return invokeFallbackMethod(joinPoint, distributedLock);
        }

        try {
            return joinPoint.proceed(); // 트랜잭션으로 관리되는 메서드가 이 안에서 실행된다.
        } finally {
            multiLock.unlock(); // 커밋 이후 락 해제
        }
    }

    private List<String> convertToKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        var method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        var context = new StandardEvaluationContext();
        var parameterNames = getParameterNames(method);

        var args = joinPoint.getArgs();
        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }
        var value = parser.parseExpression(keyExpression).getValue(context);
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of(String.valueOf(value));
    }

    private String[] getParameterNames(Method method) {
        var parameterNames = nameDiscoverer.getParameterNames(method);
        if (parameterNames == null) {
            throw new IllegalStateException(
                    "%s 의 파라미터명을 읽을 수 없습니다. 컴파일 시 '-parameters' 옵션이 적용되었는지 확인하세요."
                            .formatted(method));
        }
        return parameterNames;
    }

    private Object invokeFallbackMethod(ProceedingJoinPoint joinPoint,
                                      DistributedLock distributedLock) throws Throwable {
        var fallbackMethodName = distributedLock.fallback();
        var parameterTypes = ((MethodSignature) joinPoint.getSignature()).getParameterTypes();
        var target = joinPoint.getTarget();
        var fallbackMethod = target.getClass().getMethod(fallbackMethodName, parameterTypes);
        var args = joinPoint.getArgs();
        try {
            return fallbackMethod.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause(); // fallback 메서드의 예외 그대로 반환
        }
    }
}
