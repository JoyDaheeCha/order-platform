package com.flab.orderplatform.payment.infrastructure.config;

import com.flab.orderplatform.payment.infrastructure.message.inbox.PaymentInboxEventProcessor;
import com.flab.orderplatform.payment.infrastructure.persistence.PaymentInboxEventJpaRepository;
import com.flab.orderplatform.shared.inbox.ContextInbox;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

import static com.flab.orderplatform.shared.event.EventConstants.AGGREGATE_PAYMENT;

@Configuration
public class PaymentInboxConfig {

    @Bean
    ContextInbox paymentInbox(
            PaymentInboxEventJpaRepository jpaRepository,
            @Qualifier("paymentTransactionManager") PlatformTransactionManager transactionManager,
            List<PaymentInboxEventProcessor> processors
    ) {
        return new ContextInbox(AGGREGATE_PAYMENT, jpaRepository, transactionManager, processors);
    }
}
