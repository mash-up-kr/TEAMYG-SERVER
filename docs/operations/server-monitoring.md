# 서버 상태 리포트·과사용 알림 런북 (#142)

Prometheus 없이 cron + 셸 스크립트 + Discord 웹훅으로 서버 상태를 알린다.
코드는 `deploy/scripts/setup-server-monitoring.sh`에 있다.

## 배경

- Prometheus는 서버 용량(메모리·CPU 여유)이 부족해 도입하지 않았다. 상시 떠 있는 수집기 대신 cron이 잠깐 돌고 끝나는 셸 스크립트로 대체했다.
- 검사 주기는 10분이다. 사용률 알림은 몇 분 늦어도 괜찮고, 이 주기로 돌렸을 때의 서버 부담은 `--measure`로 설치 전후에 직접 확인한다.
- 순간적으로 치솟았다 내려오는 OOM은 사용률 검사로 놓칠 수 있어서 커널 로그를 따로 읽어 알린다.

## 무엇이 설치되나

| 서버 경로 | 내용 |
|---|---|
| `/usr/local/bin/parfait-monitor.sh` | `check` / `report` / `snapshot` 서브커맨드. 원본은 저장소의 별도 파일이 아니라 `setup-server-monitoring.sh` 안에 내장돼 있고, 설치할 때 이 경로에 만들어진다 |
| `/etc/cron.d/parfait-monitor` | 10분마다 `check`, 한국 시간 09:00·21:00에 `report`(서버 시간대가 UTC라 cron에는 `0 0,12`로 적혀 있다). 임계치 환경변수(`CPU_WARN`, `MEM_WARN`, `DISK_WARN`)도 여기에 있다 |
| `/var/lib/parfait-monitor/` | 상태 파일(권한 700). 지표별 연속 초과 횟수, 마지막 알림 시각, 알림 중 표시, OOM 중복 방지 값, 샘플(10분마다 1줄, 최근 288줄 = 48시간) |

| 알림 | 채널 | 조건 |
|---|---|---|
| 과사용 | `#alerts` | CPU 85%·메모리 90%는 10분 간격 검사에서 연속 2회 이상, 디스크 85%는 1회. 알린 뒤 4시간 쿨다운(그동안 계속 초과여도 다시 알리지 않고, 쿨다운이 지났는데도 초과면 다시 알린다). 정상으로 돌아오면 복구 알림 1회 |
| OOM kill | `#alerts` | 커널 로그에 새 OOM 줄이 생기면 즉시 (쿨다운 없음, 같은 이벤트는 체크섬으로 한 번만) |
| 정기 리포트 | `#server-report` | 한국 시간 09:00, 21:00 (서버가 UTC라 cron은 00:00, 12:00에 실행. **서버 시간대를 바꾸면 `/etc/cron.d/parfait-monitor`의 시각도 같이 바꾼다**). 코드 블록 안에 `🖥️ EC2 SERVER STATUS` 제목, Host/Time, CPU, 메모리(사용/캐시/가용/전체), swap, 디스크, 업타임, 컨테이너 수. 샘플이 있으면 최근 24시간 평균·최대(CPU, 메모리)도 붙는다 |

- 메모리 사용률은 `(MemTotal - MemAvailable) / MemTotal`이다. 캐시는 사용량으로 세지 않고 리포트에 참고용으로만 표시한다.
- 웹훅 URL은 `/home/ubuntu/TEAMYG-SERVER/.env`의 `DISCORD_ALERTS_WEBHOOK_URL`, `DISCORD_REPORT_WEBHOOK_URL`에서 읽는다.
- 전송이 실패하거나 웹훅이 없어도 스크립트는 죽지 않고 저널(`journalctl -t parfait-monitor`)에만 남긴다.
- OOM 확인은 `journalctl -k`를 쓰므로 root로 실행돼야 한다. cron.d의 `root` 항목이라 문제없다.

## 적용 순서

서버에는 저장소가 없으므로 스크립트를 직접 옮긴다.

### 1. 사전 확인 (서버)

```sh
free -m                                   # 총 메모리·swap (사양이 예상과 같은지)
df -h /                                   # 디스크
timedatectl                               # 시간대 (UTC이면 cron 00:00/12:00 = 한국 09:00/21:00. 다른 시간대면 cron 시각 조정 필요)
docker ps                                 # 컨테이너
sudo journalctl -k --since "1 hour ago" | tail -3   # 커널 로그 조회 가능 여부
ls /var/log/journal 2>/dev/null | head -1 # 비어 있으면 journald가 휘발성이라 재부팅 전 OOM 기록이 사라진다
grep -c '^DISCORD_ALERTS_WEBHOOK_URL=' /home/ubuntu/TEAMYG-SERVER/.env   # 1이면 있음 (값은 출력하지 않는다)
grep -c '^DISCORD_REPORT_WEBHOOK_URL=' /home/ubuntu/TEAMYG-SERVER/.env   # 1이면 있음
command -v /usr/bin/time || sudo apt-get install -y time                 # --measure에 필요
```

### 2. 스크립트를 서버로 옮기고 문법 확인

로컬에서 파일 내용을 복사해 서버에서 `setup-server-monitoring.sh`로 저장한 뒤:

```sh
bash -n setup-server-monitoring.sh && echo OK
```

### 3. 부담 측정 (설치 없음)

```sh
sudo bash setup-server-monitoring.sh --measure 10
```

`--measure`는 설치하지 않고 웹훅 전송과 실제 상태 기록도 하지 않지만, 반드시 `sudo`로 실행한다. 일반 사용자는 커널 로그(`journalctl -k`)를 못 읽는 경우가 많아(`adm` 그룹 구성원은 읽을 수 있다) sudo로 실행한다. 읽지 못하면 OOM 확인이 건너뛰어져 실제 cron 실행보다 CPU 시간이 조금 적게 측정된다.

- `[통과]`/`[주의]`와 `종합:` 줄을 확인한다. 기준은 1회 CPU 시간 중앙값 200ms, 벽시계 시간 3초(CPU 샘플링 1초 포함), 프로세스 최대 메모리 20MB, 하루 CPU 시간 60초, 실행 전후 가용 메모리 감소 10MB다.
- `-- 현재 지표 --` 아래에서 **현재 값이 임계치를 넘는지** 본다. `[주의] 메모리 현재 ...`가 나오면 설치 직후부터 알림이 올 수 있으므로 임계치를 올린 뒤 설치한다 (예: `sudo env MEM_WARN=95 bash setup-server-monitoring.sh`).
- `[주의]`가 나온 부담 항목이 있으면 설치하지 않고 원인을 먼저 본다 (주기를 15~30분으로 늘리는 등).

### 4. 설치

```sh
sudo bash setup-server-monitoring.sh --dry-run   # 바뀔 내용 확인
sudo bash setup-server-monitoring.sh             # 적용
sudo bash setup-server-monitoring.sh             # 한 번 더: 전부 [건너뜀]이어야 한다
```

### 5. 알림 시험

**아래 명령은 `.env`의 실제 웹훅으로 전송하므로 `#alerts`와 `#server-report`에 진짜 메시지가 올라간다.** 팀에 시험 중임을 알리고 실행한다.

```sh
# CPU_WARN=0이면 CPU가 얼마든 초과로 본다 (CPU_WARN=1은 CPU가 1% 미만인 한가한 서버에서는 초과가 아니다)
sudo env CPU_WARN=0 STATE_DIR=/tmp/parfait-test /usr/local/bin/parfait-monitor.sh check   # 1번째: 알림 없음
sudo env CPU_WARN=0 STATE_DIR=/tmp/parfait-test /usr/local/bin/parfait-monitor.sh check   # 2번째: #alerts에 과사용 알림
sudo /usr/local/bin/parfait-monitor.sh report                                              # #server-report 메시지
sudo rm -rf /tmp/parfait-test
```

- CPU·메모리는 연속 2회 초과여야 알리므로 알림은 **2번째 실행에서만** 온다. 1번째에서는 조용한 것이 정상이다.
- `STATE_DIR`를 임시 경로로 지정했으므로 실제 상태(`/var/lib/parfait-monitor`)는 건드리지 않는다. `sudo env ...`로 넘기는 것은 sudo 설정과 무관하게 환경변수가 전달되게 하기 위해서다.
- 임시 상태로 실행하므로 이 시험이 쿨다운·복구 알림 상태에 영향을 주지 않는다. 다만 새 상태 디렉터리라서 직전 10분 안에 OOM 로그가 있었다면 OOM 알림이 함께 올 수 있다.
- `report`는 샘플이 없으면 24시간 평균·최대 없이 현재 값만 보여 준다.

### 6. cron 실행 확인

다음 10분 경계(:00, :10, ...)가 지난 뒤:

```sh
sudo ls -l /var/lib/parfait-monitor/        # samples 파일이 생기고 갱신되는지
sudo tail -3 /var/lib/parfait-monitor/samples
journalctl -t parfait-monitor --since "1 hour ago"   # 오류가 없는지 (비어 있으면 정상)
grep CRON /var/log/syslog | tail -5         # cron이 실행했는지
```

## 적용 후

- 현재 지표는 언제든 `sudo /usr/local/bin/parfait-monitor.sh snapshot`으로 볼 수 있다. 값을 출력만 하고 웹훅 전송이나 상태 기록은 하지 않는다.
- 비용 재측정은 설치된 스크립트를 직접 돌리지 말고 `--measure`를 다시 실행한다. 임시 상태와 전송 차단으로 돌아가서 실제 상태·채널에 영향이 없다.

  ```sh
  sudo bash setup-server-monitoring.sh --measure 10
  ```

- 일주일 뒤 `journalctl -t parfait-monitor --since "7 days ago"`와 `#alerts`의 알림 횟수가 과하지 않은지 본다.

## 임계치·주기 조정

`/etc/cron.d/parfait-monitor`를 편집한다 (재시작 불필요).

```sh
sudo nano /etc/cron.d/parfait-monitor     # CPU_WARN, MEM_WARN, DISK_WARN, CONSECUTIVE, COOLDOWN_SEC 값 또는 */10 주기
```

설치 스크립트를 다시 실행하면 이 파일이 기본값으로 덮어써진다. 다시 실행할 때는 `sudo env MEM_WARN=95 bash setup-server-monitoring.sh`처럼 환경변수로 같은 값을 넘긴다.
연속 횟수(`CONSECUTIVE`, 기본 2)와 쿨다운(`COOLDOWN_SEC`, 기본 14400초 = 4시간)도 같은 방식으로 cron 파일에 기록된다. 예: `sudo env CONSECUTIVE=3 COOLDOWN_SEC=7200 bash setup-server-monitoring.sh`.

## 롤백

```sh
sudo rm -f /etc/cron.d/parfait-monitor /usr/local/bin/parfait-monitor.sh
sudo rm -rf /var/lib/parfait-monitor
```

## 한계

- 서버 자체가 죽으면 알림이 오지 않는다 (#143 또는 외부 서비스로 보완).
- journald가 휘발성이면 재부팅 전의 OOM 기록은 볼 수 없다.
- 전송이 실패하면(HTTP 오류, 웹훅 URL 없음) 상태를 기록하지 않으므로 다음 10분 실행에서 같은 알림을 다시 시도한다. 웹훅 URL이 없으면 실제로 보낼 것이 생겼을 때(알림, 복구, OOM, 리포트)만 저널에 `웹훅 변수가 비어 있어 전송하지 않음`이 남는다.
- 컨테이너 개수·죽음 알림은 하지 않는다 (`--restart unless-stopped`와 #144가 맡는다).
