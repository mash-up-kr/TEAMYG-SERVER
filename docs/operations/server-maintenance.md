# 서버 유지보수 런북 (#145)

컨테이너 자동 복구, 로그·이미지 누적으로 인한 디스크 고갈 방지, 이전 이미지로의 롤백 절차를 다룬다.
관련 코드는 `deploy/scripts/`와 `.github/workflows/rollback.yml`에 있다.

## 배경 (2026-10-05 실측)

| 항목 | 결과 |
|---|---|
| 서버 | Ubuntu, cron `active`, `/etc/docker/daemon.json` 없음(기본 설정) |
| 컨테이너 | `parfait`(ECR 이미지), `caddy`(`caddy:2`). 둘 다 `--restart always`, 로그 제한 없음 |
| journald | 사용량 256.8MB, `SystemMaxUse` 미설정 |
| 디스크(정리 전) | 28G 중 22G 사용(76%). 이미지 67개 중 활성 2개(16.26GB), 태그 없는 이미지 60개 |
| 디스크(정리 후) | 7.2G 사용(26%), 여유 21G (태그 없는 이미지, 멈춘 컨테이너, 미사용 이미지 수동 정리. 아래 "수동 정리 기록") |

원인은 배포마다 `docker pull ...:latest`가 태그를 새 이미지로 옮기고 이전 이미지가 태그 없는 상태로 남는 것이다.
정리 단계가 없어 배포 횟수만큼 쌓였다. 이슈의 `docker system prune` 대신 **태그 없는 이미지 중 최근 2개만 남기고 삭제**한다.

- `system prune`은 멈춘 컨테이너·네트워크까지 지워 범위가 넓다.
- 서버에 남은 이전 이미지가 롤백의 유일하게 쉬운 수단이므로 전부 지우지 않는다.
- 시간 기준(`until=`)은 배포가 오래 없을 때 직전 버전이 지워질 수 있어 개수 기준으로 한다.

## 1. 서버 설정 적용

`deploy/scripts/setup-server-maintenance.sh`는 아래를 멱등으로 적용한다 (이미 같은 내용이면 건너뛴다).

| 대상 | 내용 |
|---|---|
| `/etc/systemd/journald.conf.d/99-parfait.conf` | `SystemMaxUse=500M` 후 `systemd-journald` 재시작 |
| `/usr/local/bin/parfait-image-prune.sh` | 태그 없는 이미지 중 최근 `KEEP`(기본 2)개를 남기고 삭제한다. 원본은 저장소의 별도 파일이 아니라 `setup-server-maintenance.sh` 안(`prune_script_content`)에 내장돼 있고, 서버에서 설정 스크립트를 실행할 때 이 경로에 만들어진다. |
| `/etc/cron.d/parfait-docker-prune` | 매주 일요일 04:00(서버 시간대)에 위 스크립트 실행, 로그는 `/var/log/parfait-docker-prune.log` |

### 적용 전 확인

`docker image ls`가 생성 시각 최신순으로 나열한다는 전제로 "최근 2개"를 고른다. 서버에서 순서를 먼저 확인한다.

```sh
docker image ls -f dangling=true --format '{{.ID}} {{.CreatedSince}}'
```

위에서부터 `CreatedSince`가 짧은(최근) 순서면 정상이다.

### 처음 적용할 때 체크리스트

1. 서버에서 이미지 정렬을 확인한다 (위 "적용 전 확인").
2. 스크립트를 서버로 옮긴다 (아래 "서버로 스크립트 옮기기").
3. `sudo bash /tmp/setup-server-maintenance.sh --dry-run`으로 바뀔 내용만 확인한다.
   `[dry-run] 작성 예정:` 3개(journald 드롭인, 정리 스크립트, cron)가 보이면 정상이다.
4. 문제없으면 `--dry-run`을 빼고 `sudo bash /tmp/setup-server-maintenance.sh`로 적용한다.
5. 아래 "재시작 정책"과 "적용 확인"을 실행한다.

### 서버로 스크립트 옮기기

코드를 푸시해도 서버에는 스크립트가 전달되지 않는다 (서버에는 저장소가 없고 `deploy.yml`도 이 파일을 보내지 않는다).
서버에 접속한 세션(SSM 세션 등)에서 아래 중 하나로 옮긴다.

**방법 1: 복사·붙여넣기 (기본)**

```sh
sudo nano /tmp/setup-server-maintenance.sh   # 저장소의 deploy/scripts/setup-server-maintenance.sh 내용을 붙여넣고 저장
bash -n /tmp/setup-server-maintenance.sh && echo SYNTAX_OK
```

`SYNTAX_OK`가 나오지 않으면 붙여넣기가 깨진 것이니 방법 2를 쓴다.
`cat > 파일 <<'EOF'`로 붙여넣으면 안 된다 (스크립트 안에 이미 `EOF`가 있어 입력이 중간에 끊긴다).

**방법 2: base64 한 줄로 붙여넣기 (깨지지 않음)**

로컬(저장소 루트)에서 클립보드에 복사한다.

```sh
base64 < deploy/scripts/setup-server-maintenance.sh | tr -d '\n' | pbcopy
```

서버에서 `<붙여넣기>` 자리에 붙여넣는다.

```sh
echo '<붙여넣기>' | base64 -d > /tmp/setup-server-maintenance.sh
bash -n /tmp/setup-server-maintenance.sh && echo SYNTAX_OK
```

**방법 3: SSM 전송 (로컬에 AWS CLI 인증이 있을 때만)**

```sh
B64=$(base64 < deploy/scripts/setup-server-maintenance.sh | tr -d '\n')
cat > /tmp/ssm-params.json <<JSON
{"commands":[
  "echo $B64 | base64 -d > /tmp/setup-server-maintenance.sh",
  "sudo bash /tmp/setup-server-maintenance.sh --dry-run"
]}
JSON
aws ssm send-command --region ap-northeast-2 \
  --instance-ids i-0a3e0094147db6031 \
  --document-name AWS-RunShellScript \
  --parameters file:///tmp/ssm-params.json \
  --query "Command.CommandId" --output text
```

결과 확인 (위 명령이 출력한 CommandId 사용):

```sh
aws ssm get-command-invocation --region ap-northeast-2 \
  --command-id <CommandId> --instance-id i-0a3e0094147db6031 \
  --query "{Status:Status, Out:StandardOutputContent, Err:StandardErrorContent}"
```

내용이 맞으면 `--dry-run`을 빼고 같은 방법으로 다시 실행한다.

### 재시작 정책 (재생성 없이 즉시)

```sh
sudo docker update --restart unless-stopped parfait caddy
```

`always`와 달리 수동으로 `docker stop`한 컨테이너는 서버나 Docker가 재시작돼도 내려간 채로 유지된다.
점검 후에는 직접 `docker start`한다.

### 로그 제한 반영

- 앱(`parfait`): `deploy.yml`의 `docker run`에 `--log-opt max-size=10m --log-opt max-file=3`이 들어 있어 **다음 배포 때** 컨테이너가 재생성되며 반영된다.
- Caddy: [HTTPS 런북의 "컨테이너 옵션 변경"](https-setup.md) 절차로 재생성할 때 반영된다. 급하지 않다
  (stdout 로그가 5주에 25MB, 접근 로그는 Caddyfile로 이미 롤링 중).

### 적용 확인

```sh
journalctl --disk-usage                                  # 이후 500M을 넘지 않는다
cat /etc/systemd/journald.conf.d/99-parfait.conf
cat /etc/cron.d/parfait-docker-prune
ls -l /usr/local/bin/parfait-image-prune.sh        # 설정 스크립트가 만든 파일 (-rwxr-xr-x)
sudo docker inspect parfait --format '{{.HostConfig.RestartPolicy.Name}} {{.HostConfig.LogConfig}}'
sudo tail /var/log/parfait-docker-prune.log              # 첫 실행(일요일 04:00) 이후
df -h /
```

### 설정 롤백

```sh
sudo rm /etc/systemd/journald.conf.d/99-parfait.conf && sudo systemctl restart systemd-journald
sudo rm /etc/cron.d/parfait-docker-prune /usr/local/bin/parfait-image-prune.sh
```

## 2. 이전 이미지로 롤백

GitHub Actions의 **Rollback App** 워크플로(`.github/workflows/rollback.yml`)를 쓴다. 서버에 접속하지 않는다.

1. Actions → Rollback App → Run workflow, `mode=list`로 실행한다.
   로그의 "현재 실행 중인 이미지"와 "서버의 이미지 목록(최신순)"을 본다.
2. 되돌릴 이미지 ID를 고른다. 목록에서 **[앱 이미지]로 표시된 것**(parfait 이미지) 중, 현재 실행 중인 이미지 다음으로 최근 것이 직전 버전이다. 앱 이미지가 아닌 ID(caddy 등)는 워크플로가 거부한다.
3. `mode=rollback`, `image=<이미지 ID>`로 실행한다. 워크플로가 이미지 존재와 현재 실행 중인 이미지와의 중복을 확인한 뒤
   `stop → rm → run`을 하고 헬스체크까지 통과해야 성공이다.

안전장치와 주의:

- 이미지 ID는 12~64자리 소문자 16진수(`sha256:` 접두사 허용)만 받는다. 그 외 값은 서버에 닿기 전에 거부된다.
- 서버에 이미지가 없거나 이미 실행 중인 이미지면 컨테이너를 건드리지 않고 중단한다.
- 롤백 직전의 실행 중 이미지 ID가 로그에 출력된다. 롤백 이후에도 문제가 있으면 그 ID로 다시 `rollback`하면 원래대로 돌아간다.
- 헬스체크가 실패하면 워크플로가 실패로 끝나며 자동 복원은 하지 않는다. 로그의 이전 이미지 ID로 다시 `rollback`한다.
- **직전 버전 자동 선택은 없다.** 롤백 후에는 방금 내린 문제 버전이 가장 최근 태그 없는 이미지가 되기 때문이다.
- **롤백해도 `main`에는 문제 커밋이 그대로 있다.** `main`에서 해당 변경을 revert하지 않으면 다음 배포가 문제 버전을 다시 올린다.
- 서버에 남는 이전 버전은 최근 2개다. 그보다 오래된 버전으로는 이 방법으로 돌아갈 수 없다.
- 롤백 후에는 실행 중인 이미지도 태그 없는 이미지가 되어 정리 대상 "최근 2개" 중 한 자리를 차지한다. 그래서 "직전 2개 버전 보존"은 근사치다.
- `mode=rollback`은 배포(`deploy.yml`)와 같은 `concurrency` 그룹이라 서로 겹쳐 실행되지 않는다. 다만 같은 그룹은 실행 1개 + 대기 1개만 허용해서, 대기 중에 새 실행이 들어오면 이전 대기 실행은 취소된다. `mode=list`는 서버를 바꾸지 않아 이 그룹을 쓰지 않는다.
- `workflow_dispatch`는 기본 브랜치에 파일이 있어야 Actions 탭에 나타난다.

비상 수단(워크플로를 쓸 수 없을 때): 서버에는 롤백 스크립트가 없으므로 SSM 세션에서 아래 명령을 복사해 직접 실행한다.
옵션은 `deploy.yml`의 `docker run`과 같다.

```sh
docker image ls                                   # 되돌릴 이미지 ID 확인
docker image inspect <ID> --format '{{json .Config.Entrypoint}}'   # ["java","-jar","app.jar"]가 나와야 앱 이미지다
sudo docker stop parfait && sudo docker rm parfait
sudo docker run -d --name parfait --network host \
  --env-file /home/ubuntu/TEAMYG-SERVER/.env \
  -v /home/ubuntu/TEAMYG-SERVER/parfait-firebase-key.json:/app/parfait-firebase-key.json:ro \
  --restart unless-stopped --log-opt max-size=10m --log-opt max-file=3 \
  <ID>
curl -sf http://localhost:8080/health && echo OK
```

`<ID>`가 앱 이미지가 아니면(caddy 등) `parfait`가 내려간 채 남으니 두 번째 줄의 확인을 건너뛰지 않는다.

## 3. 수동 정리 기록

서버에 쌓여 있던 잔재는 자동화하지 않고 서버에서 직접 정리했다. 정리 전 확인한 근거는 아래와 같다.

| 대상 | 정리 근거 |
|---|---|
| 멈춘 컨테이너 `strange_satoshi` | 4개월 전 `Exited (1)`한 일회성 실행 잔재 |
| `gradle:8.14-jdk21` (1.25GB) | 이미지 빌드는 GitHub 러너에서 하고 서버는 ECR에서 pull만 하므로 서버에서는 불필요 |
| `mysql:8.0` (1.1GB) | 3306을 리슨하는 프로세스가 없고 앱 DB 주소가 외부 호스트라 불필요 |
| 로컬 `parfait:latest` (457MB) | 4개월 전 수동 빌드. 운영 이미지(ECR, 최근 배포)와 별개 |
| `python:3.11-slim` (189MB) | 저장소·cron·실행 컨테이너 어디에도 쓰는 곳이 없음 |

결과: 사용 22G(76%) → 9.4G(34%) → 7.2G(26%), 여유 21G.
`eclipse-temurin:21-jre-jammy`는 현재 앱 이미지의 베이스 레이어라 남겼다.

`docker image rm <이미지>`는 컨테이너가 쓰는 이미지면 거부하므로 안전장치가 된다.
이미지 정리는 볼륨을 건드리지 않으므로 Caddy 인증서·접근 로그(`/home/ubuntu/caddy/data`)는 영향이 없다.
