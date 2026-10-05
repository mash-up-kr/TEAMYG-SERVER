#!/usr/bin/env bash
# 서버 유지보수 설정(#145)을 적용한다. 몇 번 실행해도 안전하다(멱등).
#   - journald 로그 용량 제한 (드롭인 파일)
#   - 태그 없는 Docker 이미지 정리 스크립트 설치 + 주 1회 cron 등록
#
# 사용법: sudo bash setup-server-maintenance.sh [--dry-run]
#   --dry-run  파일을 쓰지 않고 바뀔 내용만 출력한다.
#
# 서버에는 저장소가 없으므로 이 파일을 서버로 직접 옮겨 실행한다.
# 옮기는 방법과 처음 적용할 때의 순서는 docs/operations/server-maintenance.md를 따른다.
#
# 테스트용: PARFAIT_ROOT를 지정하면 모든 경로가 그 아래로 바뀌고 root 확인·systemctl 재시작을 건너뛴다.
set -euo pipefail

ROOT="${PARFAIT_ROOT:-}"
JOURNALD_CONF="${ROOT}/etc/systemd/journald.conf.d/99-parfait.conf"
PRUNE_BIN="${ROOT}/usr/local/bin/parfait-image-prune.sh"
CRON_FILE="${ROOT}/etc/cron.d/parfait-docker-prune"
PRUNE_LOG="/var/log/parfait-docker-prune.log"

DRY_RUN=0
for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN=1 ;;
    *)
      echo "알 수 없는 옵션: $arg (사용법: setup-server-maintenance.sh [--dry-run])" >&2
      exit 2
      ;;
  esac
done

if [[ -z "$ROOT" && "$DRY_RUN" -eq 0 && "$EUID" -ne 0 ]]; then
  echo "root 권한이 필요하다: sudo bash $0" >&2
  exit 1
fi

journald_content() {
  cat <<'EOF'
[Journal]
SystemMaxUse=500M
EOF
}

# 파일명에 점(.)이 있으면 Debian의 cron이 /etc/cron.d 파일을 무시하므로 점을 쓰지 않는다.
cron_content() {
  cat <<EOF
# parfait: 태그 없는 Docker 이미지 정리 (최근 2개 보존). deploy/scripts/setup-server-maintenance.sh가 관리한다.
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
0 4 * * 0 root /usr/local/bin/parfait-image-prune.sh >> ${PRUNE_LOG} 2>&1
EOF
}

prune_script_content() {
  cat <<'PRUNE_EOF'
#!/usr/bin/env bash
# 태그 없는(dangling) Docker 이미지 중 최근 KEEP개를 남기고 삭제한다.
# deploy/scripts/setup-server-maintenance.sh가 설치하고 cron이 주 1회 실행한다.
#   KEEP  남길 개수 (기본 2). 롤백(.github/workflows/rollback.yml)에 쓸 이전 버전을 남기기 위함이다.
# 사용 중인 이미지는 docker image rm이 거부하므로 지워지지 않는다.
set -u -o pipefail

KEEP="${KEEP:-2}"
if [[ ! "$KEEP" =~ ^[0-9]+$ ]]; then
  echo "KEEP은 0 이상의 정수여야 한다: $KEEP" >&2
  exit 2
fi

echo "$(date '+%F %T') 이미지 정리 시작 (KEEP=$KEEP)"

# docker image ls는 생성 시각 최신순으로 출력한다.
if ! out="$(docker image ls -f dangling=true -q)"; then
  echo "docker image ls 실패" >&2
  exit 1
fi

ids=()
while IFS= read -r id; do
  if [[ -n "$id" ]]; then
    ids+=("$id")
  fi
done < <(printf '%s\n' "$out" | awk '!seen[$0]++')

total="${#ids[@]}"
if (( total <= KEEP )); then
  echo "태그 없는 이미지 ${total}개: 삭제할 것 없음"
  exit 0
fi

removed=0
skipped=0
for id in "${ids[@]:KEEP}"; do
  if docker image rm "$id" > /dev/null 2>&1; then
    echo "삭제: $id"
    removed=$((removed + 1))
  else
    echo "건너뜀(사용 중이거나 삭제 실패): $id"
    skipped=$((skipped + 1))
  fi
done
echo "완료: 삭제 ${removed}개, 건너뜀 ${skipped}개"
PRUNE_EOF
}

# 전역 CHANGED: 0 = 이미 같은 내용(권한·소유자만 다시 적용), 1 = 파일을 썼거나(dry-run이면 쓸 예정)
CHANGED=0

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
    CHANGED=0
    return 0
  fi

  CHANGED=1
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

install_file "$JOURNALD_CONF" 644 "$(journald_content)"
journald_changed="$CHANGED"
install_file "$PRUNE_BIN" 755 "$(prune_script_content)"
install_file "$CRON_FILE" 644 "$(cron_content)"

if (( journald_changed )) && (( ! DRY_RUN )) && [[ -z "$ROOT" ]]; then
  systemctl restart systemd-journald
  echo "[적용] systemd-journald 재시작"
fi
