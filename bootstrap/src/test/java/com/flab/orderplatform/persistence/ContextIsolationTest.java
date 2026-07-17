package com.flab.orderplatform.persistence;

import com.flab.orderplatform.order.infrastructure.persistence.PingEntity;
import com.flab.orderplatform.order.infrastructure.persistence.PingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import jakarta.persistence.EntityManagerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Day 2 완료 기준 — 컨텍스트 경계가 <b>규약이 아니라 물리</b>임을 증명한다. (ADR-0004 B-2)
 *
 * <p>ADR-0004 는 B-1(단일 DataSource + {@code @Table(schema=)})을 "경계가 리뷰 의존"이라는
 * 이유로 기각하고 B-2 를 골랐다. 그 선택의 근거가 실행 가능한 증거로 남는 자리가 여기다.
 */
@SpringBootTest
@DisplayName("컨텍스트 영속화 경계 (ADR-0004 B-2)")
class ContextIsolationTest {

    @DynamicPropertySource
    static void dataSources(DynamicPropertyRegistry registry) {
        MySqlTestContainer.registerDataSources(registry);
    }

    @Autowired
    private PingRepository pingRepository;

    @Autowired
    @Qualifier("orderEntityManagerFactory")
    private EntityManagerFactory orderEntityManagerFactory;

    @Autowired
    @Qualifier("paymentEntityManagerFactory")
    private EntityManagerFactory paymentEntityManagerFactory;

    @Autowired
    @Qualifier("inventoryEntityManagerFactory")
    private EntityManagerFactory inventoryEntityManagerFactory;

    /**
     * 완료 기준 ① — DataSource·EMF·TransactionManager 배선이 order 스키마에 <b>실제로 쓴다</b>.
     *
     * <p>부팅 성공만으로는 부족하다. 멀티 TransactionManager 의 대표 사고는 리포지토리가 엉뚱한
     * TM 아래서 돌아 <b>{@code save()} 는 성공한 척하고 DB 엔 아무것도 없는</b> 것이다(EntityManager 가
     * 그 트랜잭션에 join 되지 않아 flush·commit 이 안 일어난다). 저장 후 <b>다시 읽어야</b> 잡힌다.
     */
    @Test
    @DisplayName("① 더미 엔티티가 order 스키마에 실제로 저장된다")
    void savesIntoOrderSchema() {
        pingRepository.save(new PingEntity("ping-1", "hello"));
        pingRepository.flush();

        assertThat(pingRepository.findById("ping-1"))
                .as("save() 가 성공한 척만 하면 여기서 비어 있다 — TM 오배선의 전형")
                .get()
                .extracting(PingEntity::getNoteText)
                .isEqualTo("hello");
    }

    /**
     * 완료 기준 ② — <b>타 컨텍스트의 EntityManager 에 order 엔티티가 보이지 않는다.</b>
     *
     * <p>"권한이 없다"가 아니라 <b>클래스의 존재 자체를 모른다</b>. 각 EMF 의 Metamodel 은 자기
     * {@code packagesToScan} 결과가 전부라, payment 코드가 order 엔티티를 조회하려 해도 매핑이
     * 없어 SQL 을 만들 방법이 없다. cross-schema 접근이 <b>물리적으로 불가능</b>하다는 말의 실체다.
     *
     * <p>비교 대상이 있어야 성립하는 단언이다 — order 에는 있고 나머지엔 없다는 대비가 증거다.
     * (테이블은 필요 없다. Metamodel 은 부팅 시점의 매핑 정보라 DB 상태와 무관하다.)
     */
    @Test
    @DisplayName("② order 엔티티는 order 의 EntityManager 에만 존재한다")
    void otherContextsCannotSeeOrderEntities() {
        assertThat(entityNamesOf(orderEntityManagerFactory))
                .as("order 의 EMF 는 자기 엔티티를 안다")
                .contains(PingEntity.class.getName());

        assertThat(entityNamesOf(paymentEntityManagerFactory))
                .as("payment 의 EMF 에 order 엔티티가 보이면 경계는 규약일 뿐이다")
                .doesNotContain(PingEntity.class.getName());

        assertThat(entityNamesOf(inventoryEntityManagerFactory))
                .as("inventory 의 EMF 에 order 엔티티가 보이면 경계는 규약일 뿐이다")
                .doesNotContain(PingEntity.class.getName());
    }

    private static java.util.List<String> entityNamesOf(EntityManagerFactory emf) {
        return emf.getMetamodel().getEntities().stream()
                .map(type -> type.getJavaType().getName())
                .toList();
    }
}
