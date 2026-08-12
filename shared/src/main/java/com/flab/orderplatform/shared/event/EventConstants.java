package com.flab.orderplatform.shared.event;

/**
 * 공통 이벤트 관리시 사용되는 상수
 */
public final class EventConstants {

    public static final String AGGREGATE_ORDER = "order";
    public static final String AGGREGATE_PAYMENT = "payment";
    public static final String AGGREGATE_INVENTORY = "inventory";
    // --- Order ---
    public static final String ORDER_CREATED = "OrderCreated";
    public static final String ORDER_CREATED_TOPIC = "MSG-ORDER-CREATED";
    public static final String ORDER_PAID = "OrderPaid";
    public static final String ORDER_PAID_TOPIC = "MSG-ORDER-PAID";
    public static final String ORDER_CONFIRMED = "OrderConfirmed";
    public static final String ORDER_CONFIRMED_TOPIC = "MSG-ORDER-CONFIRMED";
    public static final String ORDER_CANCELLATION_REQUESTED = "OrderCancellationRequested";
    public static final String ORDER_CANCELLATION_REQUESTED_TOPIC = "MSG-ORDER-CANCELLATION-REQUESTED";
    public static final String ORDER_CANCELLED = "OrderCancelled";
    public static final String ORDER_CANCELLED_TOPIC = "MSG-ORDER-CANCELLED";
    // --- Payment ---
    public static final String PAYMENT_COMPLETED = "PaymentCompleted";
    public static final String PAYMENT_COMPLETED_TOPIC = "MSG-PAYMENT-COMPLETED";
    public static final String PAYMENT_FAILED = "PaymentFailed";
    public static final String PAYMENT_FAILED_TOPIC = "MSG-PAYMENT-FAILED";
    public static final String PAYMENT_REFUNDED = "PaymentRefunded";
    public static final String PAYMENT_REFUNDED_TOPIC = "MSG-PAYMENT-REFUNDED";
    // --- Inventory ---
    public static final String STOCK_DEDUCTED = "StockDeducted";
    public static final String STOCK_DEDUCTED_TOPIC = "MSG-STOCK-DEDUCTED";
    public static final String STOCK_SHORTAGE = "StockShortage";
    public static final String STOCK_SHORTAGE_TOPIC = "MSG-STOCK-SHORTAGE";
    public static final String STOCK_RESTORED = "StockRestored";
    public static final String STOCK_RESTORED_TOPIC = "MSG-STOCK-RESTORED";
    private EventConstants() {
        throw new UnsupportedOperationException("상수 클래스는 인스턴스화할 수 없습니다.");
    }

    /**
     * 카프카 헤더키
     */
    public static final class Headers {
        public static final String EVENT_ID = "eventId";
        public static final String EVENT_TYPE = "eventType";
        public static final String AGGREGATE_TYPE = "aggregateType";
        public static final String OCCURRED_AT = "occurredAt";
        private Headers() {
        }
    }


}
