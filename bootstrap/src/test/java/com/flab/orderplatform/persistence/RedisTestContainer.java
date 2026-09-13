package com.flab.orderplatform.persistence;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 분산락(Redisson) 통합 테스트가 공유하는 단일 Redis 컨테이너.
 * {@link MySqlTestContainer} 와 동일하게, 컨테이너 기동이 비싸므로 전 테스트가 하나를 공유한다.
 */
public final class RedisTestContainer {

    private static final GenericContainer<?> INSTANCE =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")) // docker-compose와 같은 이미지
                    .withExposedPorts(6379);

    static {
        INSTANCE.start();
    }

    private RedisTestContainer() {
    }

    /**
     * redisson-spring-boot-starter 가 읽는 {@code spring.data.redis.*} 프로퍼티에 컨테이너 좌표를 심는다.
     */
    public static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", INSTANCE::getHost);
        registry.add("spring.data.redis.port", INSTANCE::getFirstMappedPort);
    }
}
