---
description: 현재 브랜치의 커밋으로 PR 본문을 작성하고 gh pr create로 생성한다
argument-hint: "[선택: PR 추가 설명]"
allowed-tools: Bash, AskUserQuestion, Read
---

# /pr — Pull Request 생성

현재 브랜치의 커밋과 연결 이슈로 **PR 본문을 작성하고 `gh pr create`로 생성**한다.
필요하면 push는 자동 처리한다. 커밋 생성·수정은 범위 밖(`/done` 사용).

> **선행 로드**: 본문 작성(6단계) 전에 아래 두 문서를 **반드시 Read**한다. 규칙은 여기서 재정의하지 않는다.
> - **[`docs/conventions/pull-request.md`](../../docs/conventions/pull-request.md)** — 제목 포맷·base·이슈 연결·섹션 주체·mermaid·draft·assignee/리뷰어 규칙
> - `.github/PULL_REQUEST_TEMPLATE.md` — 본문 스켈레톤

## 1. 사전 검증

막히면 해당 메시지 출력 후 **중단**:

- **워킹 트리** — `git status --porcelain` 이 비어있지 않으면
  → `❌ 커밋되지 않은 변경사항이 있습니다. 먼저 /done으로 정리 후 다시 실행해주세요.`
- **gh 인증** — `gh auth status` 실패 → `GitHub CLI 인증이 필요합니다. gh auth login 후 다시 시도해주세요.`
- **보호 브랜치 경고(차단 아님)** — `git branch --show-current` 가 `main`/`develop`이면
  → `⚠️ 보호 브랜치(<브랜치>)에서 PR을 생성합니다. 그대로 진행합니다.`
- **base 결정** — `docs/conventions/pull-request.md`대로 `main` 고정 (당분간 `develop` 미사용).
- **커밋 존재** — `git fetch origin <base>` 후 `git rev-list --count origin/<base>..HEAD` 가 0이면
  → `❌ <base> 대비 앞선 커밋이 없습니다.`

## 2. 원격 동기화 (자동 푸시)

upstream이 없거나 로컬이 원격보다 앞서 있으면:
```sh
git push -u origin "$(git branch --show-current)"
```
실패 시 이유 출력 후 중단.

## 3. 중복 PR 체크

`gh pr list --head "<branch>" --state open --json number,url,title` 결과가 있으면 **생성하지 않고** 기존 URL 출력 후 종료:
`ℹ️ 이미 열린 PR이 있습니다: <URL>`

## 4. 연결 이슈

브랜치명에서 이슈 번호를 파싱한다(포맷은 `docs/conventions/branch-naming.md` 참조). 실패 시 AskUserQuestion(번호 직접 입력 / 이슈 없음).
`N`이 있으면 `gh issue view <N> --json title,body,labels,state` — **제목·prefix는 PR 제목 조립에**, 본문·라벨은 작업내용 작성 시 참고. 여러 이슈 연결 규칙은 [`docs/conventions/pull-request.md`](../../docs/conventions/pull-request.md)를 따른다.

## 5. 커밋 로그

```sh
git log origin/<base>..HEAD --pretty=format:'%h %s%n%b%n---' --reverse
```
"작업내용" 섹션의 기초 자료로 사용.

## 6. 본문 작성

`.github/PULL_REQUEST_TEMPLATE.md`의 섹션 구조를 그대로 따른다.
**어느 섹션을 Claude가 채우는지, mermaid 자동 삽입 여부·타입·상한은 모두 [`docs/conventions/pull-request.md`](../../docs/conventions/pull-request.md)를 따른다.** 사용자 작성 섹션은 헤더+힌트 주석만 유지하고 내용을 채우지 않는다.

## 7. 미리보기 & 생성

제목·본문 전체를 보여주고 AskUserQuestion: `ready로 생성` / `draft로 생성` / `본문 수정`(반영 후 재확인) / `취소`.

제목은 `docs/conventions/pull-request.md` 포맷(`[<Prefix>/#<N>] <이슈 제목>`)으로 조립한다. 리뷰어는 CODEOWNERS로 자동 요청되므로 `--reviewer`를 지정하지 않는다.

```sh
gh pr create \
  --base "<base>" --head "<branch>" \
  --title "[<Prefix>/#<N>] <이슈 제목>" \
  --assignee @me \
  ${draft:+--draft} \
  --body-file - <<'EOF'
<구성된 본문>
EOF
```
생성된 PR URL 출력.

## 8. 결과

```
✅ PR 생성 완료
- 제목: <제목>
- base: <base> ← head: <branch>
- 상태: <draft|ready> · Assignee: @me
- 연결 이슈: #<N> (또는 없음)
- URL: <PR URL>
```
스크린샷·기타 섹션은 웹 UI나 `gh pr edit`으로 채워달라고 안내.

## 취소

어느 단계에서든 취소 시, 이미 수행된 `git push`는 되돌리지 않되 PR은 생성하지 않고 종료.
