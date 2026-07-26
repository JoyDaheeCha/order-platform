package com.flab.orderplatform.order.support;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 멱등키 통합 테스트가 공유하는 단일 Redis 컨테이너.
 */
public final class RedisTestContainer {

    private static final GenericContainer<?> INSTANCE =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")) // docker-compose와 같은 이미지. 로컬과 CI의 동작 일치시킴
                    .withExposedPorts(6379);

    static {
        INSTANCE.start();
    }

    private RedisTestContainer() {
    }

    /** 포트는 호스트에 랜덤 매핑되므로 기동 후에만 알 수 있다. */
    public static String address() {
        return "redis://%s:%d".formatted(INSTANCE.getHost(), INSTANCE.getFirstMappedPort());
    }
}
