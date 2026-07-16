# .claude — 이 레포의 Claude Code 설정

JSON 은 주석을 못 달아서, `settings.json` 의 의도를 여기에 적는다.

## Stop 훅 — todo.md 체크박스 자동 갱신

Claude 가 응답을 마칠 때마다 [`scripts/verify-day1.sh`](../scripts/verify-day1.sh) 를
`--apply --quiet` 로 실행한다. Day 1 완료 기준을 검사하고 **통과한 항목만** `todo.md` 에 체크한다.

### 알고 써야 할 것

**훅은 "완료"를 판단하지 못한다.** 셸 커맨드일 뿐이라 결정론적 검사만 한다.
그래서 항목마다 검증 방식이 다르다:

| 항목 | 검증 방식 | 훅이 자동으로 켜나 |
|------|-----------|--------------------|
| docker-compose.yml | 파일에 mysql·kafka·redis 이미지가 정의됐나 | ✅ 즉시 |
| 모듈 starter | 각 컨텍스트 모듈이 web·jpa·kafka 를 선언했나 | ✅ 즉시 |
| application.yml + local | 두 파일 존재 + `default: local` 선언 | ✅ 즉시 |
| **부팅 확인** | `localhost:8080/actuator/health` 가 UP 인가 | ⚠️ **앱이 떠 있을 때만** |

마지막 항목이 훅의 한계다. "부팅이 됐었다"는 과거 사실을 훅이 알 방법은 앱을 직접 띄우는 것뿐인데,
그건 30초가 걸려서 매 응답마다 할 일이 아니다. 그래서 훅은 8080 을 살짝 찔러보고,
꺼져 있으면 **미확인으로 남긴다(실패로 처리하지 않는다)**.

확실하게 검증하려면 직접 돌린다 — 인프라 상태를 확인하고 앱을 띄워 health 까지 본 뒤 되돌려 놓는다:

```bash
./scripts/verify-day1.sh --full --apply
```

### 설계 원칙

- **체크는 켜기만 하고 끄지 않는다.** 과거에 검증된 사실을 훅이 임의로 지우면 안 된다.
- **훅은 조용하다.** 체크박스가 바뀔 때만 `todo.md` 에 흔적이 남으므로, 무슨 일이 있었는지는
  `git diff todo.md` 로 확인한다.
- 검증의 알맹이는 훅이 아니라 **스크립트**다. 훅은 그걸 편하게 부르는 껍데기일 뿐이다.
  Day 2~16 도 같은 방식으로 `verify-dayN.sh` 를 추가하고 훅에 한 줄 붙이면 된다.

### 훅을 끄고 싶다면

`settings.json` 의 `hooks` 블록을 지우거나, 개인 설정(`settings.local.json`, git 미추적)으로 덮어쓴다.
