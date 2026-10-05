#!/usr/bin/env bash
# 앱 컨테이너(parfait)를 서버에 남아 있는 이미지로 되돌린다.
# .github/workflows/rollback.yml이 SSM으로 이 파일을 서버에 보내 실행한다.
#
# 사용법:
#   rollback-app.sh list                 서버의 이미지 목록과 현재 실행 중인 이미지를 출력한다.
#   rollback-app.sh rollback <image-id>  지정한 이미지로 컨테이너를 교체한다.
#   rollback-app.sh validate <image-id>  이미지 ID 형식을 검사하고 정규화한 값을 출력한다.
#
# 주의: docker run 옵션은 .github/workflows/deploy.yml과 같아야 한다. 한쪽을 고치면 다른 쪽도 확인한다.
# 주의: 롤백해도 main에는 문제 커밋이 남아 있다. main에서 revert하지 않으면 다음 배포가 문제 버전을 다시 올린다.
set -euo pipefail

CONTAINER="${CONTAINER:-parfait}"
ENV_FILE="${ENV_FILE:-/home/ubuntu/TEAMYG-SERVER/.env}"
FIREBASE_KEY="${FIREBASE_KEY:-/home/ubuntu/TEAMYG-SERVER/parfait-firebase-key.json}"
HEALTH_URL="${HEALTH_URL:-http://localhost:8080/health}"
HEALTH_RETRIES="${HEALTH_RETRIES:-30}"
HEALTH_INTERVAL="${HEALTH_INTERVAL:-5}"

usage() {
  cat >&2 <<'EOF'
사용법:
  rollback-app.sh list
  rollback-app.sh rollback <image-id>
  rollback-app.sh validate <image-id>
EOF
}

# 이미지 ID(12~64자리 소문자 16진수, sha256: 접두사 허용)를 검사하고 접두사 없는 값을 출력한다.
normalize_image_id() {
  local raw="${1:-}"
  local id="${raw#sha256:}"
  if [[ "$id" == *$'\n'* ]] || [[ ! "$id" =~ ^[a-f0-9]{12,64}$ ]]; then
    echo "이미지 ID 형식이 올바르지 않다 (12~64자리 소문자 16진수, sha256: 접두사 허용): ${raw}" >&2
    return 1
  fi
  printf '%s' "$id"
}

# 앱 이미지(ENTRYPOINT가 java -jar app.jar)인지 확인한다. caddy 같은 다른 이미지를 잘못 고르는 일을 막는다.
is_app_image() {
  local entrypoint
  entrypoint="$(docker image inspect --format '{{json .Config.Entrypoint}}' "$1" 2>/dev/null || true)"
  [[ "$entrypoint" == *'"app.jar"'* ]]
}

cmd_list() {
  local running short
  running="$(docker container inspect --format '{{.Image}}' "$CONTAINER" 2>/dev/null || true)"
  short="${running#sha256:}"
  short="${short:0:12}"

  echo "== 현재 실행 중인 이미지 =="
  echo "${short:-없음}"
  echo
  echo "== 서버의 이미지 목록 (최신순, [앱 이미지]만 롤백 대상) =="
  docker image ls --format '{{.ID}}\t{{.Repository}}:{{.Tag}}\t{{.CreatedSince}}\t{{.Size}}' |
    while IFS=$'\t' read -r id rest; do
      if is_app_image "$id"; then
        printf '%s\t%s\t[앱 이미지]\n' "$id" "$rest"
      else
        printf '%s\t%s\n' "$id" "$rest"
      fi
    done
}

cmd_rollback() {
  local input="${1:-}"
  if [[ -z "$input" ]]; then
    usage
    exit 2
  fi

  local id target running
  id="$(normalize_image_id "$input")"

  if ! target="$(docker image inspect --format '{{.Id}}' "$id" 2>/dev/null)"; then
    echo "서버에 이미지가 없다: $id" >&2
    exit 1
  fi

  if ! is_app_image "$id"; then
    echo "앱(parfait) 이미지가 아니다: $id (list에서 [앱 이미지]로 표시된 이미지만 롤백할 수 있다)" >&2
    exit 1
  fi

  running="$(docker container inspect --format '{{.Image}}' "$CONTAINER" 2>/dev/null || true)"
  if [[ "$target" == "$running" ]]; then
    echo "이미 실행 중인 이미지다: $id" >&2
    exit 1
  fi

  if [[ ! -s "$FIREBASE_KEY" ]]; then
    echo "firebase 키가 없다: $FIREBASE_KEY" >&2
    exit 1
  fi

  if [[ ! -r "$ENV_FILE" ]]; then
    echo "env 파일을 읽을 수 없다: $ENV_FILE" >&2
    exit 1
  fi

  echo "현재 실행 중인 이미지(되돌릴 때 사용): ${running:-없음}"
  echo "롤백 대상 이미지: $target"

  docker stop "$CONTAINER" || true
  docker rm "$CONTAINER" || true
  if ! docker run -d --name "$CONTAINER" --network host \
    --env-file "$ENV_FILE" \
    -v "$FIREBASE_KEY:/app/parfait-firebase-key.json:ro" \
    --restart unless-stopped \
    --log-opt max-size=10m --log-opt max-file=3 \
    "$target"; then
    echo "docker run 실패: 앱 컨테이너가 없는 상태다." >&2
    if [[ -n "$running" ]]; then
      echo "이전 이미지로 복구하려면 이 ID로 rollback한다: ${running#sha256:}" >&2
    fi
    exit 1
  fi

  local i
  for ((i = 1; i <= HEALTH_RETRIES; i++)); do
    if curl -sf --max-time 2 "$HEALTH_URL" > /dev/null; then
      echo "헬스체크 통과"
      return 0
    fi
    if (( i < HEALTH_RETRIES )); then
      sleep "$HEALTH_INTERVAL"
    fi
  done

  echo "헬스체크 실패: $HEALTH_URL" >&2
  if [[ -n "$running" ]]; then
    echo "이전에 실행 중이던 이미지로 되돌리려면 이 ID로 다시 rollback한다: ${running#sha256:}" >&2
  fi
  exit 1
}

main() {
  case "${1:-}" in
    list) cmd_list ;;
    validate)
      normalize_image_id "${2:-}"
      echo
      ;;
    rollback) cmd_rollback "${2:-}" ;;
    *)
      usage
      exit 2
      ;;
  esac
}

main "$@"
