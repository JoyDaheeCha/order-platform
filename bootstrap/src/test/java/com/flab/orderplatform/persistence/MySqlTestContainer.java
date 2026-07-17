package com.flab.orderplatform.persistence;

import java.nio.file.Path;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * 컨텍스트 3개가 공유하는 단일 MySQL 컨테이너. (ADR-0004 A-2: 단일 인스턴스 + 스키마 분리)
 *
 * <p><b>왜 {@code @ServiceConnection} 을 안 쓰나</b>: 그건 컨테이너 URL 을
 * {@code spring.datasource.*} 에 꽂아주는데, 우리는 {@code DataSourceAutoConfiguration} 을 껐다.
 * 꽂아줘도 읽는 주체가 없다 — 자동설정을 끄면 그 위에 얹힌 생태계의 편의도 같이 꺼진다.
 * 그래서 {@code @DynamicPropertySource} 로 우리 prefix 3개에 직접 매핑한다.
 *
 * <p><b>왜 init 스크립트를 컨테이너에 마운트하나</b>: docker-compose 와 <b>같은 메커니즘</b>
 * (MySQL 이미지가 {@code /docker-entrypoint-initdb.d/} 의 *.sql 을 순서대로 실행)을 쓰면
 * 스키마 초기화의 출처가 하나가 된다. {@code withInitScript()} 는 스크립트 1개만 받으므로
 * (우리는 01-schemas + 02-ping) 디렉토리째 복사한다.
 */
public final class MySqlTestContainer {

    /** 컨테이너 기동이 비싸므로 전 테스트가 하나를 공유한다(JVM 종료 시 Ryuk 이 정리). */
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

    /** 컨테이너 기본 DB(test)가 아니라 우리가 init 으로 만든 스키마를 가리키게 바꾼다. */
    private static String jdbcUrl(String schema) {
        return "jdbc:mysql://%s:%d/%s".formatted(
                INSTANCE.getHost(), INSTANCE.getFirstMappedPort(), schema);
    }
}
