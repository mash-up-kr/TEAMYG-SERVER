# FCM 서비스 계정 키 배치 (1회성)

> `dev`/`prod` 컨테이너는 `file:/app/parfait-firebase-key.json` 에서 FCM 자격증명을 읽는다.
> 이 파일이 없으면 `FcmConfig` 에서 부팅 실패하고 deploy.yml 헬스체크가 떨어져 배포가 중단된다.

## 절차

1. Firebase 콘솔 → 프로젝트 설정 → 서비스 계정 → **새 비공개 키 생성** → JSON 다운로드.
2. EC2(`parfait-server`)에 접속:
   ```sh
   scp parfait-firebase-key.json ubuntu@<EC2>:/home/ubuntu/TEAMYG-SERVER/parfait-firebase-key.json
   ```
   (`.env` 와 같은 폴더. jar·도커 이미지·git 어디에도 넣지 않는다.)
3. 권한 최소화:
   ```sh
   chmod 600 /home/ubuntu/TEAMYG-SERVER/parfait-firebase-key.json
   ```
4. 다음 배포부터 deploy.yml 이 이 파일을 `/app/parfait-firebase-key.json:ro` 로 마운트한다.

## 확인

배포 후:
```sh
sudo docker exec parfait ls -l /app/parfait-firebase-key.json
curl -sf http://localhost:8080/health
```
