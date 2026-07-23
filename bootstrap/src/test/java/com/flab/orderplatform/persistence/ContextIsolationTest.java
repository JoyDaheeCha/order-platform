package com.flab.orderplatform.persistence;

import com.flab.orderplatform.order.domain.Order;
import com.flab.orderplatform.order.infrastructure.persistence.OrderJpaRepository;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static com.flab.orderplatform.order.domain.status.OrderStatus.PENDING;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("컨텍스트 영속화 경계가 order, payment, inventory로 나눠졌는지 테스트")
class ContextIsolationTest {

    @DynamicPropertySource
    static void dataSources(DynamicPropertyRegistry registry) {
        MySqlTestContainer.registerDataSources(registry);
    }

    @Autowired
    private OrderJpaRepository orderRepository;

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
        var order = orderRepository.save(new Order("order-1", 12_000L, LocalDateTime.now(), PENDING, List.of(), 100L));
        orderRepository.flush();

        assertThat(orderRepository.findById(order.getId()))
                .as("save() 가 성공한 척만 하면 여기서 비어 있다")
                .get()
                .extracting(Order::getTotalAmount)
                .isEqualTo(12_000L);
    }

    @Test
    @DisplayName("order 엔티티는 order 의 EntityManager 에만 존재한다")
    void otherContextsCannotSeeOrderEntities() {
        assertThat(entityNamesOf(orderEntityManagerFactory))
                .as("order 의 EMF 는 자기 엔티티를 안다")
                .contains(Order.class.getName());

        assertThat(entityNamesOf(paymentEntityManagerFactory))
                .as("payment 의 EMF 에 order 엔티티가 보이면 경계는 규약일 뿐이다")
                .doesNotContain(Order.class.getName());

        assertThat(entityNamesOf(inventoryEntityManagerFactory))
                .as("inventory 의 EMF 에 order 엔티티가 보이면 경계는 규약일 뿐이다")
                .doesNotContain(Order.class.getName());
    }

    private static java.util.List<String> entityNamesOf(EntityManagerFactory emf) {
        return emf.getMetamodel().getEntities().stream()
                .map(type -> type.getJavaType().getName())
                .toList();
    }
}
