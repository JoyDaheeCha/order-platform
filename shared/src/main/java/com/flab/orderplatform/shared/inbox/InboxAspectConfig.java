package com.flab.orderplatform.shared.inbox;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class InboxAspectConfig {
    @Bean
    InboxAspect inboxAspect(List<ContextInbox> contextInboxes) {
        return new InboxAspect(contextInboxes);
    }
}
