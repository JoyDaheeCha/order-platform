# 프로젝트 요약
주문 플랫폼의 분산 환경 시스템을 구현

## 프로젝트 내 기능
- POST /orders 
- GET /orders/{id} 
- POST /orders/{id}/cancel

## 목적
- 도메인 유즈케이스는 단순화 하되, 분산환경에서 사용하는 다양한 기법 (예. SAGA 패턴, 동시성) 을 익힌다
- 부하테스트에서 트래픽을 충분히 견디도록 설계한다. 

# TODO
##  환경세팅
- [x] 앱이 뜨고, MySQL·Kafka·Redis에 붙는다.
- [x] 주문, 재고, 결제 컨텍스트마다 자기 스키마만 확인할 수 있음을 테스트로 증명한다. 

## 주문 생성(POST /orders) api 추가 구현 목표 (day3~day6)

- [x] 테이블 추가
    - [x] order_item : 상품 id, 주문번호, 가격 snapshot (주문 당시 가격)
    - [x] product : 상품 id, 상품코드, 가격 (상품 도메인에서 받아오는 상품 메타정보)  
          ** 상품 도메인의 정보는 본 프로젝트에는 테이블에 일괄적으로 넣어둔다. 
              실무에서는 상품 도메인의 값을 카프카 메시지로 최신화하지만
              본 프로젝트에서는 매번 동기화되고 있다고 가정한다.
- [x] 주문을 생성하면 PENDING 주문이 DB에 저장된다 
 - [x] prod
 - [x] test
- [x] 주문 생성 중복 요청을 방지
    - [x] 같은 멱등키 재요청은 기존 주문을 반환한다.
    - [x] 같은 키에 다른 페이로드는 409로 거부한다. 
- [x] backlog
  - [x] redis callback 패턴 추가 (order 테이블에 impotent key 저장 후 유니크 인덱스 추가)
  - [x] Order, Order Item 양방향으로 변경할지 검토 (업데이트 쿼리 별도로 나가는지 확인)

## Outbox 패턴 추가 + '주문 생성되었다' 이벤트 발행
- [x] OrderCreated(주문이 요청되었다) 이벤트가 발행된다
- [x] outbox 패턴 추가
  - [x] order 모듈 내에서 메시지 발행시 Outbox 테이블 거쳐 발행되도록 도메인 이벤트 발행 로직 구성
  - [x] 인스턴스가 두개 이상일때 한번만 스케줄러가 돌도록 shedLock 추가
- [x] outbox 재발행 스케줄러 주가 (failed 상태 재발행)
- [x] outbox 재발행 스케줄러 주가 (created 상태 재발행)
- [x] outbox 테이블에서 발송완료 데이터 제거 로직 추가
- [x] 테이블 인덱스 적용 (status, created_at) 복합키  

## Inbox 패턴 추가 + '주문 생성되었다' 이벤트 컨슈밍
- [x] 결제 - 인박스 패턴 추가
- [x] 결제 - 주문 생성되었다 이벤트 컨슈밍 로직 추가

## 환경 설정 & 기타
[x] flyway 추가
[x] docs 하위 문서 사람에게 가독성있게 간략화 (ai 전용 문서는 ./claude 하위로 옮길것)

## 주문 조회 api(GET /orders/{id}) 추가
- [ ] **read model** `order_saga_progress` + `GET /orders/{id}` 폴링 조회 (ADR-0004, PC-4)
- 
## 주문 취소 api 추가

## 결재 완료 메시지 발행
- [x] 결재 완료 메시지 발행
- [x] 주문 도메인에서 수신, 상태값 변경

## 주문 결제 완료 이벤트 발행 (OrderPaid)
- [x] 주문 이벤트에서 발행 (결제에서 직접 발행하지 않는 이유: 결제에서는 상품 정보를 재고로 넘기지 않아야함)
- [x] 결제 도메인에서 수신
- [ ] 문서 수정

# 재고에서 재고 차감 이벤트 발행
- [ ] 재고 변경 메시지 발행
  - [x] 재고 차감 성공시 StockDeducted(재고가 차감되었다) 발행
  - [ ] 실패시 StockDeductionFailed(재고 차감이 실패하였다) 발행
- [ ] 주문에서 이를 수신
  - 주문 확정 처리
  - 주문에서 OrderConfirmed(주문이 확정되었다) 발행
  - order_saga_process 테이블 진행도 변경
- [ ] GET 폴링으로 주문 확정여부 확인

## 리팩토링
- [x] shared 로 기능 공통화
- [x] order 인박스 패턴도 스케줄러로 일원화

## 주문 플로우 변경 (Since 9/3)
### 주문 생성
- [x] 주문이 생성되었을때, 재고에서는 
  - [x] 가용재고가 충분하다면
    - [x] 가용재고를 감소시키고 재고를 선점한다
    - [x] 재고선점 완료 메시지를 발행한다.
  - [x] 가용 재고가 부족하거나, 유효한 요청이 아닐 경우
    - [x] 재고 선점 실패 메시지를 발행한다. 
- [x] 재고 선점이 완료되었을때 주문에서는 (product-spec 4.1~4.2)
  - [x] 재고 선점 스케줄링용 데이터를 등록한다
  - [x] 1분마다 동작, 10분 내 미결제시 재고 선점 해제)
  - [x] 결제 준비 완료 메시지를 발행한다

## feature-todo5-pay 작업 내용
** 기존 작업 내용을 아래와 같이 변경  
결제도메인 

- [x] `OrderPaymentPrepared` 이벤트 컨슈밍  
- [x] 결제시도  (기존)
  - 결제 상태 `IN_PROGRESS` 로 변경  
  - PG사 mock API 호출

- [x] 결제 성공  (기존)
  - 결제 상태 `COMPLETED`로 변경  
  - `PaymentCompleted` 발행

- [x] 결제 실패  
  - 결제 상태 `FAILED`로 변경  
  - `PaymentFailed` 발행

## 주문: 결제완료, 실패
- [x] 결제 완료처리  
 - 기존 로직 
 - `PaymentCompleted` 컨슘 
 - `PAID` 로 주문 상태 변경 
 - `OrderPaid` 발행 
 - [x] `OrderInventoryReservation` 에서 선점 해제(isReleased = true, reason = PAYMENT_COMPLETED) 처리

- [x] 결제 실패처리   
  - [x] `PaymentFailed` 컨슘 
  - [x] `ORDER_FAILED` 로 주문 상태 변경  
  - [x] `OrderInventoryReservation` 에서 선점 해제(isReleased = true, reason = PAYMENT_FAILED) 처리  (기)
  - [x] `OrderFailed` 발행

## 7. 재고:  원복 or 차감
재고 차감
 - `OrderPaid` 컨슘
 - `inventory` 테이블에서 `reservation_count`(선점 재고 수량) 를 선점 했던 수량만큼 **차감**
 - `InventoryDeducted` 이벤트 발행

재고 원복
-`OrderFailed` 컨슘  
`inventory` 테이블에서
- `reservation_count`(선점 재고 수량) 를 선점 했던 수량만큼 **차감**
- `stock`(가용재고수량) 컬럼 수량 **증량**

동시성 처리: 분산락

# backlog
## 카프카 메시지 재처리 로직 추가
- [ ] 메시지 처리 실패시 별도 테이블에 적재후, 개발자가 수기처리하도록 로직 구성
  - [ ] DLQ 와 장단점 비교해볼것

## 모니터링 대시보드 추가
- [ ] 그라파나
- [ ] 프로메테우스