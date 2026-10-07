#!/usr/bin/env bash
# 서버 보안 최소 조치(#147)를 적용한다. 몇 번 실행해도 안전하다(멱등).
#   - fail2ban 설치와 SSH 보호 설정 (동료가 차단되지 않도록 느슨한 기본값)
#   - SSH 로그인 알림 (PAM pam_exec -> #alerts 디스코드 웹훅)
#   - 자동 보안 패치(unattended-upgrades) 활성화 확인
#
# 사용법: sudo env FAIL2BAN_IGNOREIP="<허용할 IP들, 공백 구분>" bash setup-server-security.sh [--dry-run]
#   --dry-run          파일을 쓰지 않고 바뀔 내용만 출력한다 (패키지도 설치하지 않는다).
#   FAIL2BAN_IGNOREIP  fail2ban이 절대 차단하지 않을 IP/대역(공백 구분). 필수.
#                      127.0.0.1/8 과 ::1 은 항상 포함한다. 없으면 자기·동료를 차단할 수 있어 중단한다.
#
# 서버에는 저장소가 없으므로 이 파일을 서버로 직접 옮겨 실행한다.
# 옮기는 방법과 적용 순서는 docs/operations/server-security.md를 따른다.
#
# 테스트용: PARFAIT_ROOT를 지정하면 모든 경로가 그 아래로 바뀌고 root 확인·apt-get·systemctl을 건너뛴다.
set -euo pipefail

ROOT="${PARFAIT_ROOT:-}"
JAIL_CONF="${ROOT}/etc/fail2ban/jail.d/parfait.local"
ALERT_BIN="${ROOT}/usr/local/bin/parfait-ssh-alert.sh"
PAM_SSHD="${ROOT}/etc/pam.d/sshd"
AUTO_UPGRADES="${ROOT}/etc/apt/apt.conf.d/20auto-upgrades"
PAM_LINE="session optional pam_exec.so quiet seteuid /usr/local/bin/parfait-ssh-alert.sh"

DRY_RUN=0
for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN=1 ;;
    *)
      echo "알 수 없는 옵션: $arg (사용법: setup-server-security.sh [--dry-run])" >&2
      exit 2
      ;;
  esac
done

if [[ -z "$ROOT" && "$DRY_RUN" -eq 0 && "$EUID" -ne 0 ]]; then
  echo "root 권한이 필요하다: sudo env FAIL2BAN_IGNOREIP=\"...\" bash $0" >&2
  exit 1
fi

# 차단하지 않을 IP 목록 검증. 설정 파일에 그대로 들어가므로 IP/CIDR에 쓰는 문자만 허용한다.
IGNOREIP="${FAIL2BAN_IGNOREIP:-}"
if [[ -z "${IGNOREIP// /}" ]]; then
  echo "FAIL2BAN_IGNOREIP가 필요하다 (차단하지 않을 IP, 공백 구분). 없으면 자기·동료를 차단할 수 있어 중단한다." >&2
  exit 2
fi
IGNORE_LIST="127.0.0.1/8 ::1"
set -f
for token in $IGNOREIP; do
  if [[ ! "$token" =~ ^[0-9A-Fa-f:./]+$ ]]; then
    set +f
    echo "FAIL2BAN_IGNOREIP에 올바르지 않은 값이 있다: $token" >&2
    exit 2
  fi
  IGNORE_LIST="$IGNORE_LIST $token"
done
set +f

# 동료가 실수로 차단되지 않도록 일부러 느슨하게 잡았다 (10분 안에 10번 실패하면 30분 차단).
jail_content() {
  cat <<EOF
# parfait: SSH 보호. deploy/scripts/setup-server-security.sh가 관리한다.
# 동료가 실수로 차단되지 않도록 일부러 느슨하게 설정했다 (10분 안에 10번 실패하면 30분 차단).
[DEFAULT]
ignoreip = ${IGNORE_LIST}
bantime = 30m
findtime = 10m
maxretry = 10

[sshd]
enabled = true
backend = systemd
EOF
}

alert_script_content() {
  cat <<'ALERT_EOF'
#!/usr/bin/env bash
# SSH 로그인(세션 열림)을 #alerts 디스코드 웹훅으로 알린다.
# /etc/pam.d/sshd의 pam_exec가 호출한다. 로그인을 막거나 지연시키지 않도록 항상 0으로 종료한다.
# deploy/scripts/setup-server-security.sh가 설치한다.
ENV_FILE="/home/ubuntu/TEAMYG-SERVER/.env"

[ "${PAM_TYPE:-}" = "open_session" ] || exit 0

# 외부 입력(사용자명, 접속 IP)은 안전한 문자만 남긴다 (JSON 인젝션 방지).
clean() { printf '%s' "$1" | tr -cd 'A-Za-z0-9._:-'; }

user="$(clean "${PAM_USER:-unknown}")"
rhost="$(clean "${PAM_RHOST:-unknown}")"
host="$(clean "$(hostname 2>/dev/null)")"
now="$(date '+%F %T %Z')"

# .env 전체를 source하지 않고 필요한 한 줄만 읽는다 (다른 비밀값이 섞여 있다).
url="$(grep -m1 '^DISCORD_ALERTS_WEBHOOK_URL=' "$ENV_FILE" 2>/dev/null | cut -d= -f2- | tr -d "\"'\r")"
[ -n "$url" ] || exit 0

payload="{\"embeds\":[{\"title\":\"🔐 SSH 로그인\",\"description\":\"**사용자**: ${user}\\n**접속 IP**: ${rhost}\\n**서버**: ${host}\",\"color\":3447003,\"footer\":{\"text\":\"로그인 시각: ${now}\"}}]}"
( curl -sS --max-time 5 -H 'Content-Type: application/json' -d "$payload" "$url" > /dev/null 2>&1 < /dev/null & )
exit 0
ALERT_EOF
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

install_fail2ban() {
  if dpkg -s fail2ban > /dev/null 2>&1; then
    echo "[건너뜀] 이미 설치됨: fail2ban"
    return 0
  fi
  if (( DRY_RUN )); then
    echo "[dry-run] 설치 예정: fail2ban (apt-get install -y fail2ban)"
    return 0
  fi
  if [[ -n "$ROOT" ]]; then
    echo "[건너뜀] PARFAIT_ROOT 지정: apt-get 설치 생략"
    return 0
  fi
  DEBIAN_FRONTEND=noninteractive apt-get update -qq
  DEBIAN_FRONTEND=noninteractive apt-get install -y fail2ban
  echo "[적용] fail2ban 설치"
}

# /etc/pam.d/sshd 끝에 pam_exec 한 줄을 없을 때만 덧붙인다. 파일을 통째로 다시 쓰지 않는다.
add_pam_line() {
  if [[ ! -f "$PAM_SSHD" ]]; then
    echo "[실패] PAM 파일이 없다: $PAM_SSHD" >&2
    exit 1
  fi
  if grep -qxF "$PAM_LINE" "$PAM_SSHD"; then
    echo "[건너뜀] 이미 적용됨: $PAM_SSHD"
    return 0
  fi
  if (( DRY_RUN )); then
    echo "[dry-run] 한 줄 추가 예정: $PAM_SSHD"
    echo "    | $PAM_LINE"
    return 0
  fi
  cp -p "$PAM_SSHD" "${PAM_SSHD}.parfait-bak"
  if [[ -n "$(tail -c1 "$PAM_SSHD")" ]]; then
    printf '\n' >> "$PAM_SSHD"
  fi
  printf '%s\n' "$PAM_LINE" >> "$PAM_SSHD"
  echo "[적용] $PAM_SSHD (백업: ${PAM_SSHD}.parfait-bak)"
}

# 자동 보안 패치가 이미 켜져 있으면 건너뛰고, 꺼져 있을 때만 설정 파일을 쓴다.
ensure_auto_upgrades() {
  if [[ -f "$AUTO_UPGRADES" ]] && grep -q 'APT::Periodic::Unattended-Upgrade "1"' "$AUTO_UPGRADES"; then
    echo "[건너뜀] 이미 켜져 있음: 자동 보안 패치 ($AUTO_UPGRADES)"
    return 0
  fi
  install_file "$AUTO_UPGRADES" 644 'APT::Periodic::Update-Package-Lists "1";
APT::Periodic::Unattended-Upgrade "1";'
}

if [[ -z "$ROOT" && "$EUID" -eq 0 ]] && command -v sshd > /dev/null 2>&1; then
  if ! sshd -T 2>/dev/null | grep -qi '^usepam yes'; then
    echo "[경고] sshd의 UsePAM이 yes가 아니다. SSH 로그인 알림이 동작하지 않을 수 있다." >&2
  fi
fi

install_fail2ban
install_file "$JAIL_CONF" 644 "$(jail_content)"
jail_changed="$CHANGED"
install_file "$ALERT_BIN" 755 "$(alert_script_content)"
add_pam_line
ensure_auto_upgrades

if (( jail_changed )) && (( ! DRY_RUN )) && [[ -z "$ROOT" ]]; then
  systemctl restart fail2ban
  echo "[적용] fail2ban 재시작"
fi

if (( DRY_RUN )); then
  echo "[dry-run] 완료: 파일을 하나도 쓰지 않았다."
else
  echo "적용 완료. 지금 열려 있는 SSH 세션은 닫지 말고, 새 터미널에서 SSH로 접속해 로그인이 되는지와 #alerts 알림이 오는지 확인한다."
fi
