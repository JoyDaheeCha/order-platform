package com.flab.orderplatform.order.domain;

import java.util.ArrayList;
import java.util.List;

public class DomainEventThreadManager {
    private static final ThreadLocal<List<DomainEvent>> eventThreadLocal = ThreadLocal.withInitial(ArrayList::new);

    private DomainEventThreadManager() {
        throw new UnsupportedOperationException("DomainEventThreadManager 는 static 클래스로 객체 생성할 수 없습니다.");
    }

    public static void register(DomainEvent event) {
        if (event != null) {
            eventThreadLocal.get().add(event);
        }
    }

    public static List<DomainEvent> getEvents() {
        return eventThreadLocal.get();
    }

    public static void clear() {
        eventThreadLocal.get().clear();
    }
}
