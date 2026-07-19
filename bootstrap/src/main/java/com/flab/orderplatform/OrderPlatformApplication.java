package com.flab.orderplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

/**
 * 데이터소스를 3개로 컨텍스트별로 분리하였으므로,
 * DataSource·EntityManagerFactory·TransactionManager 를 직접 설정합니다.
 *
 * 따라서 DataSourceAutoConfiguration, HibernateJpaAutoConfiguration 는 exclude 합니다.
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
})
public class OrderPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderPlatformApplication.class, args);
    }
}
