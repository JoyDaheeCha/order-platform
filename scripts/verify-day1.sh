#!/usr/bin/env bash
# verify-day1.sh — todo.md Day 1 완료 기준을 "실제로" 검증한다.
#
# 왜 이런 스크립트가 필요한가:
#   체크박스를 손으로 켜는 건 "됐다고 믿는 것"이지 "된 것"이 아니다.
#   이 스크립트는 각 항목을 기계적으로 확인하고, 통과한 것만 todo.md 에 체크한다.
#   (한 번 켜진 체크는 절대 끄지 않는다 — 과거에 검증된 사실을 지우지 않기 위해)
#
# 사용법:
#   ./scripts/verify-day1.sh              # 검사만 하고 결과 출력
#   ./scripts/verify-day1.sh --apply      # 통과 항목을 todo.md 에 체크
#   ./scripts/verify-day1.sh --full       # 인프라·런타임까지 실제로 확인 (느림, 수동용)
#   ./scripts/verify-day1.sh --quiet      # 출력 최소화 (훅에서 사용)
#
# 종료 코드: 0 = 전 항목 통과 / 1 = 미완 항목 있음
#
# ⚠️ 설계상의 한계 (알고 쓸 것):
#   "부팅 확인" 항목은 앱이 지금 떠 있어야만 검증된다. 기본 모드는 8080 을 1초만 찔러보고,
#   응답이 없으면 "미확인"으로 남긴다(실패가 아니다). 앱을 직접 띄워 확인하려면 --full 을 쓴다.

set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TODO="$ROOT/todo.md"

APPLY=0; FULL=0; QUIET=0
for arg in "$@"; do
  case "$arg" in
    --apply) APPLY=1 ;;
    --full)  FULL=1 ;;
    --quiet) QUIET=1 ;;
    *) echo "알 수 없는 옵션: $arg" >&2; exit 2 ;;
  esac
done

say() { [ "$QUIET" -eq 1 ] || echo -e "$1"; }

PASS_COUNT=0; FAIL_COUNT=0
TICKS=()

# check <결과> <항목이름> <todo.md 체크박스 문구>
check() {
  local ok="$1" name="$2" needle="$3"
  if [ "$ok" -eq 0 ]; then
    say "  ✅ $name"
    PASS_COUNT=$((PASS_COUNT + 1))
    TICKS+=("$needle")
  else
    say "  ❌ $name"
    FAIL_COUNT=$((FAIL_COUNT + 1))
  fi
}

say "\n── Day 1 완료 기준 검증 ──────────────────────────────"

# ── 1. docker-compose.yml: MySQL·Kafka·Redis 3종이 정의됐나 ────────────────
ok=1
if [ -f "$ROOT/docker-compose.yml" ] \
  && grep -q "image: mysql:" "$ROOT/docker-compose.yml" \
  && grep -q "image: apache/kafka:" "$ROOT/docker-compose.yml" \
  && grep -q "image: redis:" "$ROOT/docker-compose.yml"; then
  ok=0
fi
check $ok "docker-compose.yml — MySQL·Kafka·Redis 정의" \
  '`docker-compose.yml` — MySQL 8 · Kafka(KRaft 단일 노드) · Redis (design.md §5: DB는 스키마로만 격리)'

# ── 2. 5개 모듈 build.gradle 에 starter 가 채워졌나 ────────────────────────
# "필요한 starter"의 정의: 컨텍스트 3모듈은 web·jpa·kafka 어댑터를, bootstrap 은 실행 스타터를 갖는다.
# (shared 는 의존성 0 이 의도된 설계라 검사 대상에서 뺀다 — architecture.md §4)
ok=0
for m in order payment inventory; do
  f="$ROOT/$m/build.gradle"
  [ -f "$f" ] \
    && grep -q "spring.boot.starter.web" "$f" \
    && grep -q "spring.boot.starter.data.jpa" "$f" \
    && grep -q "spring.kafka" "$f" || ok=1
done
grep -q "spring.boot.starter" "$ROOT/bootstrap/build.gradle" 2>/dev/null || ok=1
[ -f "$ROOT/shared/build.gradle" ] || ok=1
check $ok "5개 모듈 build.gradle starter" \
  '5개 모듈 `build.gradle`에 필요한 starter 채우기 (libs.versions.toml 참조)'

# ── 3. application.yml 골격 + local 프로파일 ──────────────────────────────
ok=1
RES="$ROOT/bootstrap/src/main/resources"
if [ -f "$RES/application.yml" ] && [ -f "$RES/application-local.yml" ] \
  && grep -q "default: local" "$RES/application.yml"; then
  ok=0
fi
check $ok "application.yml 골격 + local 프로파일 분리" \
  '`application.yml` 기본 골격 + `local` 프로파일'

# ── 4. 부팅 확인 — 여기만 성격이 다르다 ───────────────────────────────────
# 앱이 살아 있어야 검증된다. 기본 모드는 살짝 찔러보고 없으면 "미확인"으로 넘어간다.
# --full 은 인프라 상태까지 확인한 뒤 실제로 bootRun 을 띄워 검증한다.
boot_ok=1
probe_health() {
  curl -sf --max-time 2 http://localhost:8080/actuator/health 2>/dev/null \
    | grep -q '"status":"UP"'
}

if probe_health; then
  boot_ok=0
  say "  ✅ OrderPlatformApplication 부팅 확인 (실행 중인 앱의 health 가 UP)"
elif [ "$FULL" -eq 1 ]; then
  say "  ⏳ 앱이 떠 있지 않다 → 인프라 확인 후 직접 띄워본다 (--full)"
  healthy=$(docker compose -f "$ROOT/docker-compose.yml" ps --format '{{.Status}}' 2>/dev/null | grep -c healthy)
  if [ "${healthy:-0}" -ne 3 ]; then
    say "  ❌ 컨테이너 3개가 healthy 가 아니다 (현재 ${healthy:-0}개) — 'docker compose up -d' 먼저"
  else
    say "     컨테이너 3개 healthy 확인 → bootRun 기동 중..."
    ( cd "$ROOT" && ./gradlew bootRun --console=plain > /tmp/verify-day1-bootrun.log 2>&1 ) &
    boot_pid=$!
    # 잡 제어에서 떼어낸다. 안 그러면 아래에서 kill 할 때 셸이 "Terminated: 15" 를 출력한다.
    disown "$boot_pid" 2>/dev/null || true
    for _ in $(seq 1 40); do
      probe_health && break
      sleep 3
    done
    if probe_health; then
      boot_ok=0
      say "  ✅ OrderPlatformApplication 부팅 확인 (직접 기동해 health UP 확인)"
    else
      say "  ❌ 부팅 실패 — 로그: /tmp/verify-day1-bootrun.log"
    fi
    # 검증용으로 띄운 앱은 되돌려 놓는다. 스크립트가 환경을 바꿔놓고 끝나면 안 된다.
    kill "$boot_pid" 2>/dev/null
    pkill -f "OrderPlatformApplication" 2>/dev/null
  fi
fi

if [ "$boot_ok" -eq 0 ]; then
  PASS_COUNT=$((PASS_COUNT + 1))
  TICKS+=('`OrderPlatformApplication` 부팅 확인')
else
  # 실패가 아니라 "지금은 알 수 없음"이다. 이미 체크돼 있다면 그대로 둔다.
  if grep -qF -- '- [x] `OrderPlatformApplication` 부팅 확인' "$TODO" 2>/dev/null; then
    say "  ✅ OrderPlatformApplication 부팅 확인 (이전에 검증됨)"
    PASS_COUNT=$((PASS_COUNT + 1))
  else
    say "  ⏸  OrderPlatformApplication 부팅 확인 — 미확인 (앱이 떠 있을 때 다시 실행하거나 --full)"
    FAIL_COUNT=$((FAIL_COUNT + 1))
  fi
fi

# ── todo.md 반영 ──────────────────────────────────────────────────────────
if [ "$APPLY" -eq 1 ] && [ ${#TICKS[@]} -gt 0 ]; then
  changed=$(python3 - "$TODO" "${TICKS[@]}" <<'PY'
import sys
path, needles = sys.argv[1], sys.argv[2:]
text = open(path, encoding="utf-8").read()
n = 0
for needle in needles:
    old = f"- [ ] {needle}"
    if old in text:
        text = text.replace(old, f"- [x] {needle}")
        n += 1
if n:
    open(path, "w", encoding="utf-8").write(text)
print(n)
PY
)
  [ "${changed:-0}" -gt 0 ] && say "\n  → todo.md 체크박스 ${changed}개 갱신"
fi

say "\n  통과 $PASS_COUNT / 미완 $FAIL_COUNT\n"
[ "$FAIL_COUNT" -eq 0 ]
