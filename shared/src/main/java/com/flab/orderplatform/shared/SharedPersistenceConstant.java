package com.flab.orderplatform.shared;

/**
 * shared 의 JPA 매핑 위치
 */
public class SharedPersistenceConstant {
    /** 각 컨텍스트 EntityManagerFactory 가 함께 스캔해야 할 패키지 */
    public static final String OUTBOX = "com.flab.orderplatform.shared.outbox";
    public static final String INBOX  = "com.flab.orderplatform.shared.inbox";

    private SharedPersistenceConstant() {}
}
