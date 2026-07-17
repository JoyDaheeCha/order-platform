package com.flab.orderplatform.persistence;

import java.nio.file.Path;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * 컨텍스트 3개가 공유하는 단일 MySQL 컨테이너
 */
public final class MySqlTestContainer {

    /** 컨테이너 기동이 비싸므로 전 테스트가 하나를 공유한다 */
    private static final MySQLContainer<?> INSTANCE = new MySQLContainer<>("mysql:8.0")
            // 스키마·권한을 init 스크립트가 직접 만든다 → root 로 붙는다(로컬 compose 와 동일).
            .withUsername("root")
            .withPassword("root")
            .withCopyFileToContainer(
                    MountableFile.forHostPath(initDir()),
                    "/docker-entrypoint-initdb.d/");

    static {
        INSTANCE.start();
    }

    private MySqlTestContainer() {
    }

    private static Path initDir() {
        String dir = System.getProperty("mysql.init.dir");
        if (dir == null) {
            throw new IllegalStateException(
                    "system property 'mysql.init.dir' 가 없다. bootstrap/build.gradle 의 test 태스크가 넘긴다.");
        }
        return Path.of(dir);
    }

    /**
     * 컨텍스트별 {@code datasource.{context}.*} 에 컨테이너 좌표를 심는다.
     * 같은 인스턴스를 보되 스키마만 다르다 — 경계는 스키마와 EntityManagerFactory 가 만든다.
     */
    public static void registerDataSources(DynamicPropertyRegistry registry) {
        register(registry, "order", "order_schema");
        register(registry, "payment", "payment_schema");
        register(registry, "inventory", "inventory_schema");
    }

    private static void register(DynamicPropertyRegistry registry, String context, String schema) {
        registry.add("datasource.%s.url".formatted(context), () -> jdbcUrl(schema));
        registry.add("datasource.%s.username".formatted(context), INSTANCE::getUsername);
        registry.add("datasource.%s.password".formatted(context), INSTANCE::getPassword);
    }

    private static String jdbcUrl(String schema) {
        return "jdbc:mysql://%s:%d/%s".formatted(
                INSTANCE.getHost(), INSTANCE.getFirstMappedPort(), schema);
    }
}
