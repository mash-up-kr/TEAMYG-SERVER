# 도메인 HTTPS 적용 런북 (#112)

EC2에 Caddy를 리버스 프록시로 올려 도메인에 Let's Encrypt 인증서를 적용한 1회성 절차의 기록.
인증서 갱신은 Caddy가 자동으로 처리하므로 재실행할 일이 없다. 서버를 새로 만들거나 도메인을
바꿀 때 이 문서를 따른다.

## 구성

```
[클라이언트] --443/TLS--> [Caddy (EC2, host network)] --8080/평문--> [parfait 컨테이너]
```

| 항목 | 값 |
|---|---|
| 도메인 | `api.parfait-app.store` (가비아 등록, 가비아 네임서버) |
| 공인 IP | `43.201.180.13` (EIP `eipalloc-036e8471df2ae16da`) |
| 인스턴스 | `i-0a3e0094147db6031` (`parfait-server`, ap-northeast-2a) |
| 보안 그룹 | `sg-015795760ae63b419` (`parfait-sg`) |
| 접근 경로 | SSM Run Command (SSH 아님) |
| 인증서 | Let's Encrypt, HTTP-01 챌린지, 만료 30일 전 자동 갱신 |

- 인증서 보관: `/home/ubuntu/caddy/data` 볼륨. **이 디렉터리를 지우면 재발급**되므로 보존한다.
  Let's Encrypt는 동일 도메인에 주당 5회 발급 제한이 있어, 반복해서 지우면 일시적으로 막힌다.
- Caddy 설정은 저장소의 [`deploy/caddy/Caddyfile`](../../deploy/caddy/Caddyfile)이 원본이다.
  애플리케이션 배포(`deploy.yml`)와 생명주기가 분리돼 있어 자동 동기화되지 않는다 —
  수정했으면 아래 "설정 변경" 절차로 직접 반영한다.

## 왜 ALB + ACM이 아닌가

ALB는 인증서 관리를 AWS에 맡길 수 있고 무중단 배포·확장에 유리하지만 월 $16~20이 든다.
단일 t2.micro 구성에서는 그 이점을 쓸 데가 없어 비용만 남는다. 나중에 필요해지면 가비아에서
네임서버를 Route53으로 위임하고 alias 레코드로 전환하면 된다.

## 절차

### 1. 공인 IP 고정

자동 할당 공인 IP는 인스턴스를 stop하면 사라진다. 도메인이 겨냥할 주소는 EIP로 고정한다.

```sh
aws ec2 associate-address --region ap-northeast-2 \
  --instance-id i-0a3e0094147db6031 \
  --allocation-id eipalloc-036e8471df2ae16da
```

연결하는 순간 기존 공인 IP는 회수된다. 그 주소를 직접 박아 쓰는 클라이언트(앱 빌드, 스크립트)가
있으면 함께 교체한다.

### 2. DNS A 레코드

가비아 DNS 관리툴에서 추가한다. 등록기관은 어디든 상관없다 — HTTP-01 챌린지는 "도메인이 이 IP를
가리키는가"만 본다.

| 타입 | 호스트 | 값 | TTL |
|---|---|---|---|
| A | `api` | `43.201.180.13` | 300 |

전파를 확인한 다음 진행한다. 여기서 IP가 다르면 인증서 발급이 실패하고 발급 횟수만 소모된다.

```sh
dig +short api.parfait-app.store @8.8.8.8
```

> Cloudflare를 경유한다면 프록시(오렌지 구름)를 끈다. 켜져 있으면 Cloudflare가 TLS를 종단해
> Caddy까지 챌린지가 오지 않는다.

### 3. 보안 그룹에 80 개방

HTTP-01 챌린지와 HTTP→HTTPS 리다이렉트에 80이 필요하다. 443은 이미 열려 있었다.

```sh
aws ec2 authorize-security-group-ingress --region ap-northeast-2 \
  --group-id sg-015795760ae63b419 \
  --ip-permissions 'IpProtocol=tcp,FromPort=80,ToPort=80,IpRanges=[{CidrIp=0.0.0.0/0,Description="ACME HTTP-01 + HTTPS redirect"}]'
```

### 4. Caddyfile 배치

EC2에 docker compose 플러그인이 없어 애플리케이션 배포와 같은 `docker run` 방식을 쓴다.
따옴표 이스케이프를 피하려고 base64로 실어 보낸다.

```sh
B64=$(base64 < deploy/caddy/Caddyfile | tr -d '\n')
cat > /tmp/ssm-params.json <<JSON
{"commands":[
  "sudo mkdir -p /home/ubuntu/caddy/data",
  "echo $B64 | base64 -d | sudo tee /home/ubuntu/caddy/Caddyfile > /dev/null",
  "sudo chown -R ubuntu:ubuntu /home/ubuntu/caddy"
]}
JSON
aws ssm send-command --region ap-northeast-2 \
  --instance-ids i-0a3e0094147db6031 \
  --document-name AWS-RunShellScript \
  --parameters file:///tmp/ssm-params.json
```

### 5. Caddy 기동

```sh
sudo docker run -d --name caddy --network host --restart always \
  -e DOMAIN=api.parfait-app.store \
  -e ACME_EMAIL=celina.16161616@gmail.com \
  -v /home/ubuntu/caddy/Caddyfile:/etc/caddy/Caddyfile:ro \
  -v /home/ubuntu/caddy/data:/data \
  caddy:2
```

발급까지 보통 10~30초 걸린다. 로그에서 확인한다.

```sh
sudo docker logs caddy 2>&1 | grep -i "certificate obtained"
```

### 6. 검증

```sh
curl -sSI https://api.parfait-app.store/health   # 200
curl -sSI http://api.parfait-app.store/health    # 308, Location: https://
echo | openssl s_client -connect api.parfait-app.store:443 \
  -servername api.parfait-app.store 2>/dev/null | openssl x509 -noout -subject -dates
```

`https://api.parfait-app.store/v3/api-docs`의 `servers[0].url`이 `https://`인지 본다.
`http://`로 나오면 `server.forward-headers-strategy` 설정이 배포에 반영되지 않은 것이다.

### 7. 8080 차단

프록시를 거치지 않는 평문 경로를 남겨 두면 HTTPS를 우회할 수 있고, 애플리케이션이
`X-Forwarded-*`를 무조건 신뢰하므로 스킴·클라이언트 IP를 위조당할 수 있다.
**6단계 검증이 끝난 뒤** 닫는다.

```sh
aws ec2 revoke-security-group-ingress --region ap-northeast-2 \
  --group-id sg-015795760ae63b419 --protocol tcp --port 8080 --cidr 0.0.0.0/0
```

규칙 전파에 몇 초 걸린다. 차단 직후 한두 번은 200이 나올 수 있으니 재시도해서 확인한다.
`deploy.yml`의 헬스체크는 인스턴스 내부에서 `http://localhost:8080/health`를 호출하므로
이 변경에 영향받지 않는다.

## 설정 변경

`deploy/caddy/Caddyfile`을 고쳤으면 4단계로 EC2에 반영한 뒤 reload한다. reload는 무중단이다.

```sh
sudo docker exec caddy caddy reload --config /etc/caddy/Caddyfile
```

## 롤백

Caddy만 내리고 8080을 다시 열면 원래 상태로 돌아온다. 인증서는 볼륨에 남으므로 재기동 시
재발급하지 않는다.

```sh
sudo docker stop caddy
aws ec2 authorize-security-group-ingress --region ap-northeast-2 \
  --group-id sg-015795760ae63b419 --protocol tcp --port 8080 --cidr 0.0.0.0/0
```

## 남은 문제

- `parfait-sg`의 22번 포트가 `0.0.0.0/0`에 열려 있다(`sgr-0aad8b77910bdf928`). 특정 IP 3개가
  이미 등록돼 있으므로 전체 개방 규칙은 제거 가능해 보인다. 이 작업 범위 밖이라 손대지 않았다.
- `.store`는 첫해 프로모션가와 갱신가 차이가 큰 TLD다. 갱신 시점에 비용을 확인한다.
