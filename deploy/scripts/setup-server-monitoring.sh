#!/usr/bin/env bash
# 서버 상태 리포트·과사용 알림을 설치한다. 몇 번 실행해도 안전하다(멱등).
#   - /usr/local/bin/parfait-monitor.sh   (check / report / snapshot)
#   - /etc/cron.d/parfait-monitor         (10분마다 check, 한국 시간 09:00·21:00 report)
#   - /var/lib/parfait-monitor/           (상태 파일)
#
# 사용법: sudo bash setup-server-monitoring.sh [--dry-run | --measure [횟수]]
#   --dry-run          파일을 쓰지 않고 바뀔 내용만 출력한다.
#   --measure [횟수]   아무것도 설치하지 않고 check를 횟수(기본 10)만큼 돌려 서버 부담을 측정한다.
#                      웹훅 전송과 상태 기록은 하지 않는다. GNU time(/usr/bin/time)이 필요하다.
#   CPU_WARN / MEM_WARN / DISK_WARN  임계치(%) 기본 85 / 90 / 85. 설치 시 cron 파일에 기록된다.
#   CONSECUTIVE / COOLDOWN_SEC       CPU·메모리 연속 초과 횟수(기본 2), 재알림 쿨다운 초(기본 14400). 마찬가지로 기록된다.
#
# 서버에는 저장소가 없으므로 이 파일을 서버로 직접 옮겨 실행한다.
#
# 테스트용: PARFAIT_ROOT를 지정하면 모든 경로가 그 아래로 바뀌고 root 확인을 건너뛴다.
#           PROC_DIR로 /proc 위치를, TIME_BIN으로 time 명령 위치를 바꿀 수 있다.
set -euo pipefail

ROOT="${PARFAIT_ROOT:-}"
MONITOR_BIN="${ROOT}/usr/local/bin/parfait-monitor.sh"
CRON_FILE="${ROOT}/etc/cron.d/parfait-monitor"
STATE_DIR="${ROOT}/var/lib/parfait-monitor"
PROC_DIR="${PROC_DIR:-/proc}"
CPU_WARN="${CPU_WARN:-85}"
MEM_WARN="${MEM_WARN:-90}"
DISK_WARN="${DISK_WARN:-85}"
CONSECUTIVE="${CONSECUTIVE:-2}"
COOLDOWN_SEC="${COOLDOWN_SEC:-14400}"

DRY_RUN=0
MEASURE=0
MEASURE_N=10
while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run) DRY_RUN=1 ;;
    --measure)
      MEASURE=1
      if [[ $# -gt 1 && "$2" =~ ^[0-9]+$ ]]; then
        MEASURE_N="$2"
        shift
      fi
      ;;
    *)
      echo "알 수 없는 옵션: $1 (사용법: setup-server-monitoring.sh [--dry-run | --measure [횟수]])" >&2
      exit 2
      ;;
  esac
  shift
done

if (( MEASURE_N < 1 )); then
  echo "--measure 횟수는 1 이상이어야 한다: $MEASURE_N" >&2
  exit 2
fi

if [[ -z "$ROOT" && "$DRY_RUN" -eq 0 && "$MEASURE" -eq 0 && "$EUID" -ne 0 ]]; then
  echo "root 권한이 필요하다: sudo bash $0" >&2
  exit 1
fi

# 파일명에 점(.)이 있으면 Debian의 cron이 /etc/cron.d 파일을 무시하므로 점을 쓰지 않는다.
cron_content() {
  cat <<EOF
# parfait: 서버 상태 리포트·과사용 알림. deploy/scripts/setup-server-monitoring.sh가 관리한다.
# 임계치를 바꾸려면 아래 값을 수정한다 (재시작 불필요).
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
CPU_WARN=${CPU_WARN}
MEM_WARN=${MEM_WARN}
DISK_WARN=${DISK_WARN}
CONSECUTIVE=${CONSECUTIVE}
COOLDOWN_SEC=${COOLDOWN_SEC}
*/10 * * * * root /usr/local/bin/parfait-monitor.sh check
# 서버 시간대가 UTC라 한국 시간 09:00·21:00을 UTC로 환산했다 (00:00, 12:00). 서버 시간대를 바꾸면 이 시각도 같이 바꾼다.
0 0,12 * * * root /usr/local/bin/parfait-monitor.sh report
EOF
}

monitor_script_content() {
  cat <<'MONITOR_EOF'
#!/usr/bin/env bash
# 서버 상태를 확인해 과사용·OOM을 #alerts로, 정기 리포트를 #server-report로 알린다.
# deploy/scripts/setup-server-monitoring.sh가 설치하고 cron이 실행한다.
#   check     10분마다: 지표 확인, 임계치 초과·OOM 알림, 복구 알림, 샘플 기록
#   report    하루 2번: 상태 요약을 #server-report로 전송
#   snapshot  현재 지표를 출력한다 (전송·상태 기록 없음)
# 웹훅 전송이 실패해도 오류로 죽지 않고 저널(`journalctl -t parfait-monitor`)에만 남긴다.
set -u -o pipefail

PROC_DIR="${PROC_DIR:-/proc}"
STATE_DIR="${STATE_DIR:-/var/lib/parfait-monitor}"
ENV_FILE="${ENV_FILE:-/home/ubuntu/TEAMYG-SERVER/.env}"
CPU_WARN="${CPU_WARN:-85}"
MEM_WARN="${MEM_WARN:-90}"
DISK_WARN="${DISK_WARN:-85}"
CONSECUTIVE="${CONSECUTIVE:-2}"
COOLDOWN_SEC="${COOLDOWN_SEC:-14400}"
NOW="${NOW_EPOCH:-$(date +%s)}"

log() { logger -t parfait-monitor -- "$*" 2> /dev/null || true; }

# 상태 파일은 숫자 한 줄만 담는다. 없거나 숫자가 아니면 기본값을 돌려준다.
state_get() { # $1 = 키, $2 = 기본값
  local v=""
  if [[ -f "$STATE_DIR/$1" ]]; then
    v="$(cat "$STATE_DIR/$1" 2> /dev/null)"
  fi
  if [[ "$v" =~ ^[0-9]+$ ]]; then echo "$v"; else echo "$2"; fi
}
# 임시 파일에 쓴 뒤 mv로 바꿔서, 디스크가 가득 차도 빈 상태 파일이 남지 않게 한다.
state_set() {
  { printf '%s\n' "$2" > "$STATE_DIR/$1.tmp" && mv -f "$STATE_DIR/$1.tmp" "$STATE_DIR/$1"; } 2> /dev/null || log "상태 파일 쓰기 실패: $1"
}
state_del() { rm -f "$STATE_DIR/$1"; }

# docker가 멈춰도 cron이 쌓이지 않도록 5초 제한을 둔다. timeout이 없으면 그대로 실행한다.
run_timeout() {
  if command -v timeout > /dev/null 2>&1; then timeout 5 "$@"; else "$@"; fi
}

meminfo_kb() { awk -v k="$1:" '$1 == k { print $2; exit }' "$PROC_DIR/meminfo" 2> /dev/null; }

# "전체 idle" 누적값. idle = idle + iowait
cpu_snapshot() {
  awk '/^cpu / { idle = $5 + $6; total = 0; for (i = 2; i <= 9; i++) total += $i; print total, idle; exit }' "$PROC_DIR/stat" 2> /dev/null
}

# 전역 변수에 현재 지표를 채운다. (CPU는 1초 간격으로 두 번 읽는다. sleep 중에는 CPU를 쓰지 않는다)
collect() {
  local t1=0 i1=0 t2=0 i2=0 dt di
  read -r t1 i1 < <(cpu_snapshot) || true
  sleep 1
  read -r t2 i2 < <(cpu_snapshot) || true
  dt=$((${t2:-0} - ${t1:-0}))
  di=$((${i2:-0} - ${i1:-0}))
  # CPU는 소수점 한 자리로 표시하려고 10배 값(CPU_X10)으로 계산한다. 임계치 비교는 정수(CPU_PCT)로 한다.
  if (( dt > 0 )); then CPU_X10=$(( 1000 * (dt - di) / dt )); else CPU_X10=0; fi
  if (( CPU_X10 > 1000 )); then CPU_X10=1000; fi
  if (( CPU_X10 < 0 )); then CPU_X10=0; fi
  CPU_PCT=$((CPU_X10 / 10))

  local total avail buffers cached swap_total swap_free
  total="$(meminfo_kb MemTotal)"; total="${total:-0}"
  avail="$(meminfo_kb MemAvailable)"; avail="${avail:-0}"
  buffers="$(meminfo_kb Buffers)"; buffers="${buffers:-0}"
  cached="$(meminfo_kb Cached)"; cached="${cached:-0}"
  swap_total="$(meminfo_kb SwapTotal)"; swap_total="${swap_total:-0}"
  swap_free="$(meminfo_kb SwapFree)"; swap_free="${swap_free:-0}"
  MEM_TOTAL_MB=$((total / 1024))
  MEM_AVAIL_MB=$((avail / 1024))
  MEM_USED_MB=$(((total - avail) / 1024))
  MEM_CACHE_MB=$(((buffers + cached) / 1024))
  SWAP_TOTAL_MB=$((swap_total / 1024))
  SWAP_USED_MB=$(((swap_total - swap_free) / 1024))
  if (( total > 0 )); then MEM_PCT=$(( 100 * (total - avail) / total )); else MEM_PCT=0; fi

  local d
  d="$(df -P / 2> /dev/null | awk 'NR == 2 { gsub("%", "", $5); print $5, $3, $2 }')"
  DISK_PCT=0; DISK_USED_KB=0; DISK_TOTAL_KB=0
  read -r DISK_PCT DISK_USED_KB DISK_TOTAL_KB <<< "$d" || true
  DISK_PCT="${DISK_PCT:-0}"; DISK_USED_KB="${DISK_USED_KB:-0}"; DISK_TOTAL_KB="${DISK_TOTAL_KB:-0}"

  UPTIME_SEC="$(awk '{ print int($1) }' "$PROC_DIR/uptime" 2> /dev/null)"
  UPTIME_SEC="${UPTIME_SEC:-0}"
}

# 리포트·snapshot에만 쓴다 (check는 docker를 부르지 않는다).
collect_containers() {
  CONTAINERS="$(run_timeout docker ps -q 2> /dev/null | grep -c . || true)"
  CONTAINERS="${CONTAINERS:-0}"
}

fmt_gb() { awk -v kb="$1" 'BEGIN { printf "%.1fG", kb / 1048576 }'; }
fmt_tenths() { echo "$(($1 / 10)).$(($1 % 10))"; } # 10배 값을 소수점 한 자리로 (625 -> 62.5)
fmt_uptime() { echo "$(($1 / 86400))일 $(($1 % 86400 / 3600))시간"; }

cmd_snapshot() {
  collect
  collect_containers
  printf 'cpu_pct=%s\ncpu_pct_precise=%s\nmem_pct=%s\nmem_used_mb=%s\nmem_cache_mb=%s\nmem_avail_mb=%s\nmem_total_mb=%s\n' \
    "$CPU_PCT" "$(fmt_tenths "$CPU_X10")" "$MEM_PCT" "$MEM_USED_MB" "$MEM_CACHE_MB" "$MEM_AVAIL_MB" "$MEM_TOTAL_MB"
  printf 'swap_used_mb=%s\nswap_total_mb=%s\ndisk_pct=%s\ncontainers=%s\nuptime_sec=%s\n' \
    "$SWAP_USED_MB" "$SWAP_TOTAL_MB" "$DISK_PCT" "$CONTAINERS" "$UPTIME_SEC"
}

# .env 전체를 source하지 않고 필요한 한 줄만 읽는다 (다른 비밀값이 섞여 있다).
read_webhook() { # $1 = 변수 이름
  [[ -f "$ENV_FILE" ]] || return 0
  grep -m1 "^$1=" "$ENV_FILE" 2> /dev/null | cut -d= -f2- | tr -d "\"'\r"
}

# Discord에서 보기 좋게 코드 블록(고정폭) 안에 제목, Host/Time 머리말, 내용을 담는다.
#   🖥️ EC2 SERVER STATUS
#   ━━━━━━━━━━━━━━━━━━━━━━
#   📍 Host    : ip-10-0-0-1
#   ⏰ Time    : 2026-10-11 16:00:01
#   ━━━━━━━━━━━━━━━━━━━━━━
#   🔥 CPU      : 6.2%
# 줄바꿈은 JSON에 넣기 위해 \n 두 글자로 쓴다. 이 스크립트가 만든 문자열만 받는다.
LINE='━━━━━━━━━━━━━━━━━━━━━━'
FENCE="$(printf '\140\140\140')"
HOST_NAME="$(hostname 2> /dev/null | tr -cd 'A-Za-z0-9._-')"

msg_body() { # $1 = 제목, $2 = 내용(줄은 \n 두 글자로 이어 붙임)
  printf '%s' "${FENCE}\\n$1\\n${LINE}\\n📍 Host    : ${HOST_NAME:-unknown}\\n⏰ Time    : $(date '+%F %T')\\n${LINE}\\n$2\\n${FENCE}"
}

# MONITOR_NO_SEND=1이면 아무것도 보내지 않는다 (setup-server-monitoring.sh --measure가 사용).
# 반환값: 0 = 보냈거나 일부러 안 보냄, 1 = 보내지 못함 (호출한 쪽은 이때 상태를 기록하지 않아 다음 실행에서 다시 시도한다).
send_embed() { # $1 = 웹훅 변수 이름, $2 = 본문(msg_body 결과), $3 = 색(10진수)
  [[ "${MONITOR_NO_SEND:-0}" == 1 ]] && return 0
  local url payload
  url="$(read_webhook "$1")"
  if [[ -z "$url" ]]; then
    log "웹훅 변수가 비어 있어 전송하지 않음: $1"
    return 1
  fi
  payload="{\"embeds\":[{\"description\":\"$2\",\"color\":$3}]}"
  if ! curl -sS --fail --max-time 5 -H 'Content-Type: application/json' -d "$payload" "$url" > /dev/null 2>&1; then
    log "웹훅 전송 실패: $1"
    return 1
  fi
  return 0
}

# 임계치 초과가 need회 연속이면 알리고, 알린 뒤에는 COOLDOWN_SEC 동안 다시 알리지 않는다.
# 알린 지표가 정상으로 돌아오면 복구 알림을 1회 보낸다.
evaluate() { # $1 = 키(cpu|mem|disk), $2 = 현재값, $3 = 임계치, $4 = 연속 필요 횟수, $5 = 표시 이름, $6 = 아이콘, $7 = 필드 이름(폭 맞춤), $8 = 화면에 보일 값(생략하면 $2)
  local m="$1" value="$2" warn="$3" need="$4" label="$5" shown="${8:-$2}" count alert_at active=0
  count="$(state_get "$m.count" 0)"
  alert_at="$(state_get "$m.alert_at" 0)"
  if [[ -f "$STATE_DIR/$m.active" ]]; then active=1; fi
  if (( value >= warn )); then
    count=$((count + 1))
    state_set "$m.count" "$count"
    if (( count >= need )) && { (( ! active )) || (( NOW - alert_at >= COOLDOWN_SEC )); }; then
      if send_embed DISCORD_ALERTS_WEBHOOK_URL "$(msg_body "$6 ${label} 과사용" "$6 $7: ${shown}%  (임계치 ${warn}%)")" 15158332; then
        state_set "$m.alert_at" "$NOW"
        state_set "$m.active" 1
      fi
    fi
  else
    state_set "$m.count" 0
    if (( active )); then
      if send_embed DISCORD_ALERTS_WEBHOOK_URL "$(msg_body "✅ ${label} 정상 복귀" "$6 $7: ${shown}%  (임계치 ${warn}%)")" 3066993; then
        state_del "$m.active"
        state_del "$m.alert_at"
      fi
    fi
  fi
}

# 10분마다 "epoch cpu(10배 값) mem disk"를 한 줄 덧붙이고 최근 288줄(48시간)만 남긴다.
record_sample() {
  local f="$STATE_DIR/samples"
  if ! printf '%s %s %s %s\n' "$NOW" "$CPU_X10" "$MEM_PCT" "$DISK_PCT" >> "$f" 2> /dev/null; then
    log "샘플 기록 실패"
    return 0
  fi
  tail -n 288 "$f" > "$f.tmp" 2> /dev/null && mv -f "$f.tmp" "$f"
}

# 최근 24시간의 "CPU평균 CPU최대 메모리평균 메모리최대 개수"를 출력한다. CPU 값은 10배 값이다. 없으면 "- - - - 0".
sample_stats() {
  local f="$STATE_DIR/samples"
  if [[ ! -f "$f" ]]; then echo "- - - - 0"; return 0; fi
  awk -v since="$((NOW - 86400))" '
    NF >= 4 && $1 ~ /^[0-9]+$/ && $1 >= since { n++; c += $2; m += $3; if ($2 > cm) cm = $2; if ($3 > mm) mm = $3 }
    END { if (n == 0) print "- - - - 0"; else printf "%d %d %d %d %d\n", c / n, cm, m / n, mm, n }
  ' "$f"
}

cmd_report() {
  collect
  collect_containers
  local cpu_avg cpu_max mem_avg mem_max n cpu_line mem_extra="" body
  read -r cpu_avg cpu_max mem_avg mem_max n <<< "$(sample_stats)"
  cpu_line="$(fmt_tenths "$CPU_X10")%"
  if (( n > 0 )); then
    cpu_line+=" (24h 평균 $(fmt_tenths "$cpu_avg")% / 최대 $(fmt_tenths "$cpu_max")%)"
    mem_extra=" (24h 평균 ${mem_avg}% / 최대 ${mem_max}%)"
  fi
  body="🔥 CPU      : ${cpu_line}"
  body+="\\n🧠 Memory   : ${MEM_PCT}%${mem_extra}"
  body+="\\n   └ 사용 ${MEM_USED_MB}MB / 캐시 ${MEM_CACHE_MB}MB / 가용 ${MEM_AVAIL_MB}MB / 전체 ${MEM_TOTAL_MB}MB"
  body+="\\n🔁 Swap     : ${SWAP_USED_MB}MB / ${SWAP_TOTAL_MB}MB"
  body+="\\n💾 Disk     : ${DISK_PCT}% ($(fmt_gb "$DISK_USED_KB") / $(fmt_gb "$DISK_TOTAL_KB"))"
  body+="\\n⏱️ Uptime   : $(fmt_uptime "$UPTIME_SEC")"
  body+="\\n🐳 Docker   : ${CONTAINERS}개 실행 중"
  send_embed DISCORD_REPORT_WEBHOOK_URL "$(msg_body "🖥️ EC2 SERVER STATUS" "$body")" 3447003
}

# 마지막 확인 이후의 커널 로그에서 OOM kill 줄을 찾는다. root 권한이 필요하다.
# 같은 이벤트를 다시 알리지 않도록 마지막으로 알린 줄의 체크섬을 저장한다.
check_oom() {
  local last lines latest sum prev count proc
  last="$(state_get oom_last_check $((NOW - 600)))"
  lines="$(journalctl -k --since "@${last}" --no-pager 2> /dev/null | grep -iE 'out of memory|oom-kill' || true)"
  if [[ -z "$lines" ]]; then
    state_set oom_last_check "$NOW"
    return 0
  fi
  latest="$(printf '%s\n' "$lines" | tail -n 1)"
  sum="$(printf '%s' "$latest" | cksum | cut -d' ' -f1)"
  prev="$(state_get oom_last_sum 0)"
  if [[ "$sum" == "$prev" ]]; then
    state_set oom_last_check "$NOW"
    return 0
  fi
  # OOM 1건이 커널 로그에 여러 줄(invoked oom-killer, oom-kill:, Killed process)로 남으므로 종료된 프로세스 줄 수를 건수로 센다.
  count="$(printf '%s\n' "$lines" | grep -c 'Killed process' || true)"
  if [[ -z "$count" || "$count" -lt 1 ]]; then count=1; fi
  proc="$(printf '%s' "$latest" | sed -n 's/.*Killed process [0-9]* (\([^)]*\)).*/\1/p' | head -n 1 | tr -cd 'A-Za-z0-9._:-')"
  # 전송에 성공했을 때만 체크섬과 확인 시각을 갱신한다. 실패하면 다음 실행에서 같은 이벤트를 다시 시도한다.
  if send_embed DISCORD_ALERTS_WEBHOOK_URL "$(msg_body "💥 OOM KILL 발생" "🎯 Process  : ${proc:-알 수 없음}\\n🔢 Count    : ${count}\\nℹ️ 마지막 확인 이후 커널이 메모리 부족으로 프로세스를 종료했습니다.")" 15158332; then
    state_set oom_last_sum "$sum"
    state_set oom_last_check "$NOW"
  fi
}

cmd_check() {
  if ! mkdir -p "$STATE_DIR" 2> /dev/null; then
    log "상태 디렉터리를 만들 수 없어 건너뜀: $STATE_DIR"
    exit 0
  fi
  chmod 700 "$STATE_DIR" 2> /dev/null || true
  # 이전 실행이 끝나지 않았으면 건너뛴다 (flock이 없는 환경에서는 생략).
  if command -v flock > /dev/null 2>&1; then
    if ! { exec 9> "$STATE_DIR/lock"; } 2> /dev/null; then
      log "락 파일을 열 수 없어 건너뜀"
      exit 0
    fi
    flock -n 9 || exit 0
  fi
  collect
  record_sample
  evaluate cpu "$CPU_PCT" "$CPU_WARN" "$CONSECUTIVE" "CPU" "🔥" "CPU      " "$(fmt_tenths "$CPU_X10")"
  evaluate mem "$MEM_PCT" "$MEM_WARN" "$CONSECUTIVE" "메모리" "🧠" "Memory   "
  evaluate disk "$DISK_PCT" "$DISK_WARN" 1 "디스크" "💾" "Disk     "
  check_oom
}

# ---- 진입점 (함수는 이 줄 위에 추가한다) ----
case "${1:-}" in
  check) cmd_check ;;
  report) cmd_report ;;
  snapshot) cmd_snapshot ;;
  *)
    echo "사용법: parfait-monitor.sh check|report|snapshot" >&2
    exit 2
    ;;
esac
MONITOR_EOF
}

install_file() {
  local path="$1" mode="$2" content="$3"

  if [[ -f "$path" ]] && [[ "$(cat "$path")" == "$content" ]]; then
    if (( ! DRY_RUN )); then
      chmod "$mode" "$path"
      if [[ "$EUID" -eq 0 ]]; then
        chown root:root "$path"
      fi
    fi
    echo "[건너뜀] 이미 적용됨: $path"
    return 0
  fi

  if (( DRY_RUN )); then
    echo "[dry-run] 작성 예정: $path"
    printf '%s\n' "$content" | sed 's/^/    | /'
    return 0
  fi

  mkdir -p "$(dirname "$path")"
  local tmp
  tmp="$(mktemp "$path.XXXXXX")"
  if ! { printf '%s\n' "$content" > "$tmp" \
      && chmod "$mode" "$tmp" \
      && { [[ "$EUID" -ne 0 ]] || chown root:root "$tmp"; } \
      && mv -f "$tmp" "$path"; }; then
    rm -f "$tmp"
    echo "[실패] 파일을 쓰지 못했다: $path" >&2
    exit 1
  fi
  echo "[적용] $path"
}

ensure_state_dir() {
  if [[ -d "$STATE_DIR" ]]; then
    if (( ! DRY_RUN )); then
      chmod 700 "$STATE_DIR"
    fi
    echo "[건너뜀] 이미 있음: $STATE_DIR"
    return 0
  fi
  if (( DRY_RUN )); then
    echo "[dry-run] 디렉터리 생성 예정: $STATE_DIR (권한 700)"
    return 0
  fi
  mkdir -p "$STATE_DIR"
  chmod 700 "$STATE_DIR"
  if [[ "$EUID" -eq 0 ]]; then
    chown root:root "$STATE_DIR"
  fi
  echo "[적용] $STATE_DIR"
}

# ---- --measure: 설치 없이 부담을 측정한다 ----
# 기준: 1회 CPU 시간 중앙값 200ms, 벽시계 시간 3초(CPU 샘플링 1초 포함), 프로세스 최대 메모리 20MB,
#       하루 총 CPU 시간 60초(144회 기준), 실행 전후 가용 메모리 감소 10MB. 넘으면 [주의]로 표시한다.
CPU_MS_LIMIT=200
WALL_LIMIT_SEC=3
RSS_LIMIT_KB=20480
DAILY_CPU_LIMIT_SEC=60
MEM_DELTA_LIMIT_KB=10240

median() { sort -n | awk '{ a[NR] = $1 } END { if (NR == 0) print 0; else print a[int((NR + 1) / 2)] }'; }
avail_kb() { awk '$1 == "MemAvailable:" { print $2; exit }' "$PROC_DIR/meminfo" 2> /dev/null || true; }

show_system() {
  echo "-- $1 --"
  if command -v free > /dev/null 2>&1; then free -m; fi
  if command -v vmstat > /dev/null 2>&1; then vmstat 1 3; fi
}

report_threshold() { # $1 = 이름, $2 = 현재값, $3 = 임계치
  local v="${2:-0}"
  if (( v >= $3 )); then
    echo "[주의] $1 현재 ${v}% >= 임계치 $3%: 설치하면 바로 알림이 갈 수 있다 (임계치를 올릴지 검토)"
  else
    echo "[정상] $1 현재 ${v}% < 임계치 $3%"
  fi
}

run_measure() {
  local n="$1" time_bin tmp bin state times i got before_kb after_kb snap
  time_bin="${TIME_BIN:-/usr/bin/time}"
  if [[ ! -x "$time_bin" ]]; then
    echo "[실패] time 명령이 없다: $time_bin (sudo apt-get install -y time)" >&2
    exit 1
  fi
  tmp="$(mktemp -d)"
  trap "rm -rf '$tmp'" EXIT
  bin="$tmp/parfait-monitor.sh"
  state="$tmp/state"
  times="$tmp/times"
  printf '%s\n' "$(monitor_script_content)" > "$bin"
  chmod 755 "$bin"
  mkdir -p "$state"
  : > "$times"

  echo "== 측정 시작: check ${n}회 (설치·웹훅 전송·실제 상태 기록 없음) =="
  show_system "측정 전"
  before_kb="$(avail_kb)"
  before_kb="${before_kb:-0}"
  for ((i = 0; i < n; i++)); do
    rm -f "$tmp/one"
    "$time_bin" -f '%e %U %S %M' -o "$tmp/one" \
      env MONITOR_NO_SEND=1 STATE_DIR="$state" PROC_DIR="$PROC_DIR" "$bin" check > /dev/null 2>&1 || true
    if [[ -f "$tmp/one" ]]; then tail -n 1 "$tmp/one" >> "$times"; fi
  done
  after_kb="$(avail_kb)"
  after_kb="${after_kb:-0}"
  show_system "측정 후"

  got="$(wc -l < "$times" | tr -d ' ')"
  if (( got < n )); then
    echo "[실패] 측정 ${n}회 중 ${got}회만 기록됐다. time 명령 출력 형식(GNU time)을 확인한다." >&2
    exit 1
  fi

  snap="$(env STATE_DIR="$state" PROC_DIR="$PROC_DIR" "$bin" snapshot 2> /dev/null || true)"
  snap_get() { printf '%s\n' "$snap" | awk -F= -v k="$1" '$1 == k { print $2 }'; }
  echo "-- 현재 지표 --"
  printf '%s\n' "$snap"
  report_threshold "CPU" "$(snap_get cpu_pct)" "$CPU_WARN"
  report_threshold "메모리" "$(snap_get mem_pct)" "$MEM_WARN"
  report_threshold "디스크" "$(snap_get disk_pct)" "$DISK_WARN"

  local cpu_ms wall rss daily delta verdict="통과"
  cpu_ms="$(awk '{ printf "%d\n", ($2 + $3) * 1000 }' "$times" | median)"
  wall="$(awk '{ print $1 }' "$times" | median)"
  rss="$(awk '{ print $4 }' "$times" | sort -n | tail -n 1)"
  daily=$(( cpu_ms * 144 / 1000 ))
  delta=$(( before_kb - after_kb ))
  if (( delta < 0 )); then delta=0; fi

  judge() { # $1 = 이름, $2 = 값+단위, $3 = 기준 문구, $4 = 통과(1)/초과(0)
    if [[ "$4" == 1 ]]; then
      echo "[통과] $1 $2 (기준 $3 이하)"
    else
      echo "[주의] $1 $2 (기준 $3 이하를 넘음)"
      verdict="주의"
    fi
  }
  echo "-- 부담 판정 --"
  judge "1회 CPU 시간 중앙값" "${cpu_ms}ms" "${CPU_MS_LIMIT}ms" "$(( cpu_ms <= CPU_MS_LIMIT ))"
  judge "1회 벽시계 시간 중앙값" "${wall}초" "${WALL_LIMIT_SEC}초" "$(awk -v w="$wall" -v l="$WALL_LIMIT_SEC" 'BEGIN { print (w <= l) ? 1 : 0 }')"
  judge "프로세스 최대 메모리" "${rss}KB" "${RSS_LIMIT_KB}KB" "$(( rss <= RSS_LIMIT_KB ))"
  judge "하루 총 CPU 시간(144회 기준)" "${daily}초" "${DAILY_CPU_LIMIT_SEC}초" "$(( daily <= DAILY_CPU_LIMIT_SEC ))"
  judge "실행 전후 가용 메모리 감소" "${delta}KB" "${MEM_DELTA_LIMIT_KB}KB" "$(( delta <= MEM_DELTA_LIMIT_KB ))"
  echo "종합: ${verdict}"
}

if (( MEASURE )); then
  run_measure "$MEASURE_N"
  exit 0
fi

install_file "$MONITOR_BIN" 755 "$(monitor_script_content)"
install_file "$CRON_FILE" 644 "$(cron_content)"
ensure_state_dir

if (( DRY_RUN )); then
  echo "[dry-run] 완료: 파일을 하나도 쓰지 않았다."
else
  echo "적용 완료. docs/operations/server-monitoring.md의 '알림 시험' 순서로 동작을 확인한다."
fi
