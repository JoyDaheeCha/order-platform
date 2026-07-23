package com.flab.orderplatform.order.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Spring Retry 활성화
 */
@Configuration
@EnableRetry
public class RetryConfig {
}
