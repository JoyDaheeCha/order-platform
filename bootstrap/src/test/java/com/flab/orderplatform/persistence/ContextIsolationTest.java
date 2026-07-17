package com.flab.orderplatform.persistence;

import com.flab.orderplatform.order.infrastructure.persistence.OrderEntity;
import com.flab.orderplatform.order.infrastructure.persistence.OrderJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import jakarta.persistence.EntityManagerFactory;

import java.time.LocalDateTime;

import static com.flab.orderplatform.order.infrastructure.persistence.status.OrderStatus.PENDING;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("컨텍스트 영속화 경계가 order, payment, inventory로 나눠졌는지 테스트")
class ContextIsolationTest {

    @DynamicPropertySource
    static void dataSources(DynamicPropertyRegistry registry) {
        MySqlTestContainer.registerDataSources(registry);
    }

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Autowired
    @Qualifier("orderEntityManagerFactory")
    private EntityManagerFactory orderEntityManagerFactory;

    @Autowired
    @Qualifier("paymentEntityManagerFactory")
    private EntityManagerFactory paymentEntityManagerFactory;

    @Autowired
    @Qualifier("inventoryEntityManagerFactory")
    private EntityManagerFactory inventoryEntityManagerFactory;

    @Test
    @DisplayName("order 엔티티가 order 스키마에 실제로 저장된다")
    void savesIntoOrderSchema() {
        orderJpaRepository.save(new OrderEntity("order-1", 12_000L, LocalDateTime.now(), PENDING));
        orderJpaRepository.flush();

        assertThat(orderJpaRepository.findById("order-1"))
                .as("save() 가 성공한 척만 하면 여기서 비어 있다")
                .get()
                .extracting(OrderEntity::getTotalAmount)
                .isEqualTo(12_000L);
    }

    @Test
    @DisplayName("order 엔티티는 order 의 EntityManager 에만 존재한다")
    void otherContextsCannotSeeOrderEntities() {
        assertThat(entityNamesOf(orderEntityManagerFactory))
                .as("order 의 EMF 는 자기 엔티티를 안다")
                .contains(OrderEntity.class.getName());

        assertThat(entityNamesOf(paymentEntityManagerFactory))
                .as("payment 의 EMF 에 order 엔티티가 보이면 경계는 규약일 뿐이다")
                .doesNotContain(OrderEntity.class.getName());

        assertThat(entityNamesOf(inventoryEntityManagerFactory))
                .as("inventory 의 EMF 에 order 엔티티가 보이면 경계는 규약일 뿐이다")
                .doesNotContain(OrderEntity.class.getName());
    }

    private static java.util.List<String> entityNamesOf(EntityManagerFactory emf) {
        return emf.getMetamodel().getEntities().stream()
                .map(type -> type.getJavaType().getName())
                .toList();
    }
}
