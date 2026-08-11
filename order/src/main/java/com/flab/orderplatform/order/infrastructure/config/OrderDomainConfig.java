package com.flab.orderplatform.order.infrastructure.config;

import com.flab.orderplatform.order.domain.OrderNumberGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderDomainConfig {

    @Bean
    OrderNumberGenerator orderNumberGenerator() {
        return new OrderNumberGenerator();
    }
}
