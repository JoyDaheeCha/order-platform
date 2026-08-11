package com.flab.orderplatform.shared.inbox;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Inbox {

    /**
     * 인박스가 실행되는 컨텍스트명
     * <p>
     * payment, order, inventory 중 하나
     */
    String value();
}
