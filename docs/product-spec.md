# 기획서 (v1.0)

---

## 1. 한 줄 정의

디자이너 브랜드 오픈마켓에서 **구매자가 상품을 주문하고, 결재 및 재고 관리가 이뤄지는** B2C 주문 도메인.

---

## 2. 페르소나

| 페르소나 | 행위            |
|----------|---------------|
| **구매자 (Buyer)** | 주문 생성, 주문 취소  |
---

## 3. 스코프
- happy case: 구매
  - 구매자의 **주문 생성 → 결제 → 재고 차감** 
- 구매 실패
  - 결제 실패, 재고 부족, 사용자 취소
- 주문 중복 요청
- 여러 사용자가 주문 시도시 재고가 정상 차감되고 오버셀 되지 않는다. 
---

<strike> ## 4. 핵심 유저 플로우 (Happy case) : AS-IS 
1. 구매자가 상품·수량을 골라 **주문 생성** 요청 (멱등키 동반)
2. 주문이 `PENDING` 상태로 생성되고 → **`OrderCreated`** 이벤트 발행 → 구매자에게 **즉시 `202 Accepted {orderId}` 응답**
3. 결제 컨텍스트가 구독 → 결제 시도 → 성공 시 **`PaymentCompleted`**
4. 주문 컨텍스트가 구독 -> 결제완료로 상태 변경 -> **`OrderPaid`**
5. 재고 컨텍스트가 구독 → 재고 차감 → 성공 시 **`StockDeducted`**
6. 주문이 `CONFIRMED`로 전이 → **`OrderConfirmed`** 발행
7. 구매자는 **주문 상태 조회하여 `CONFIRMED` 확인 </strike>

## 4. 유저 플로우 : TO-BE

### 1. 사용자의 주문 API 호출  
1.1  구매자가 상품·수량을 골라 **주문 생성** 요청 (멱등키 동반)

### 2. 주문 : 주문 생성
2.1 주문이 `RESERVING_INVENTORY`(재고 선점중) 상태로 생성  
2.2 구매자에게 즉시 `202 Accepted {orderId}` 응답  
2.3 `OrderCreated` 이벤트 발행 

### 3. 재고 : 재고 선점
3.1 `OrderCreated` 구독  
3.2 재고 선점 (Inventory 테이블에서 reservation_count 증가)  
3.3 선점 성공/실패 처리  
3.3.1 선점 성공시 `InventoryReserved` 이벤트 발행   
3.3.2 선점 실패시 `InventoryReservationFailed` 이벤트 발행  

### 4. 주문 : 결제 요청, 실패 처리 & 재고 선점 해제용 데이터 적재
4.1 선점에 성공하여 결제 요청 진행  
4.1.1 `InventoryReserved` 이벤트를 컨슈밍  
4.1.2 `PENDING_PAYMENT`(결제 대기중) 로 주문 상태 변경  
4.1.3 재고가 선점된 주문(OrderInventoryReservation) 테이블에 주문 데이터 적재  
4.1.4 `OrderPaymentPrepared` 이벤트 발행  

4.2 선점에 실패하여 주문 실패처리  
4.2.1 `InventoryReservationFailed` 이벤트를 컨슈밍  
4.2.2 `ORDER_FAILED`로 상태 변경  
4.2.3 `ORDER_FAIL_HISTORY` 테이블에 주문 id, 실패 사유 적재 (실패 사유: INVENTORY_SHORTAGE(재고부족))  

### 5. 결제  
5.1 `InventoryReserved` 이벤트 컨슈밍  
5.2 결제시도  
5.2.1 결제 상태 `IN_PROGRESS` 로 변경  
5.2.2 PG사 mock API 호출  

5.3 결제 성공  
5.3.1 결제 상태 `COMPLETED`로 변경  
5.3.2 `PaymentCompleted` 발행  

5.4 결제 실패  
5.4.1 결제 상태 `FAILED`로 변경  
5.4.2 `PaymentFailed` 발행  

### 6. 주문: 결제완료, 실패, 재고 선점 타임아웃 처리
6.1 결제 완료처리  
6.1.1 `PaymentCompleted` 컨슘  
6.1.2 `PAID` 로 주문 상태 변경  
6.1.3 `OrderInventoryReservation` 에서 선점 해제(isReleased = true, reason = PAYMENT_COMPLETED) 처리  
6.1.3 `OrderPaid` 발행  

6.2 결제 실패처리  
6.2.1 `PaymentFailed` 컨슘  
6.2.2 `ORDER_FAILED` 로 주문 상태 변경  
6.2.3 `OrderInventoryReservation` 에서 선점 해제(isReleased = true, reason = PAYMENT_FAILED) 처리  
6.2.4 `OrderFailed` 발행  

6.3 재고 선점 타임아웃 처리  
6.3.1 스케줄러에서 `OrderInventoryReservation` 테이블을 1분마다 체크  
6.3.2 주문시각으로부터 10분이 경과했으나, 결제가 이뤄지지 않은 경우, 선점 해제 선점 해제(isReleased = true, reason = PAYMENT_TIMEOUT) 처리  
6.3.3 `OrderFailed` 발행  

### 7. 재고:  원복 or 차감
7.1 재고 차감  
7.1.1 `OrderPaid` 컨슘  
7.1.2 `inventory` 테이블에서  
`reservation_count`(선점 재고 수량) 를 선점 했던 수량만큼 **차감**  
`stock`(가용재고수량) 컬럼 수량 **감소**  
7.1.3 `InventoryDeducted` 이벤트 발행 

7.2 재고 원복
7.2.1 `OrderFailed` 컨슘  
7.2.2 `inventory` 테이블에서
- `reservation_count`(선점 재고 수량) 를 선점 했던 수량만큼 **차감**
- `stock`(가용재고수량) 컬럼 수량 **증량**

### 8. 주문 완료 처리
8.1 `InventoryDeducted` 컨슘 
8.2 주문 상태 `CONFIRMED`로 전이

### 9. 구매자  
9.1 주문 상태 조회 api 호출시 `CONFIRMED` 상태의 주문 확인

---

## 5. 주문(Order) 상태 정의
```
                 결제실패 or 취소
        ┌──────────────────────────────────┐
        ▼                                   │
  [PENDING] ──결제완료──> [PAID] ──재고차감──> [CONFIRMED]
        │                   │
        │                   └─재고부족─> [CANCELLED] (결제 환불 보상)
        │
        └─결제실패─────────────────────> [CANCELLED]
```

| 상태 | 의미 | 진입 이벤트 |
|------|------|-------------|
| `PENDING` | 주문 생성, 결제 대기 | `OrderCreated` |
| `PAID` | 결제 완료, 재고 차감 대기 | `PaymentCompleted` |
| `CONFIRMED` | 재고까지 확정, 주문 성립 | `OrderConfirmed` |
| `CANCELLED` | 실패/취소로 종료 (보상 완료) | `OrderCancelled` |