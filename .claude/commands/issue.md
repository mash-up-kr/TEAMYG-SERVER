---
description: 대화형으로 GitHub 이슈를 생성하고 main에서 작업 브랜치를 분기한다
argument-hint: "[선택: 이슈에 대한 간단한 설명]"
allowed-tools: Bash, AskUserQuestion, Read
---

# /issue — 대화형 이슈 생성

사용자와 단계별 대화로 GitHub 이슈를 만들고, `main`에서 작업 브랜치까지 분기한다. (당분간 `develop` 미사용)
아래 순서대로 따르고, 한 단계라도 실패·취소되면 즉시 중단한다.

> **선행 로드**:
> - 이슈 유형이 정해지면(2단계) `.github/ISSUE_TEMPLATE/`의 해당 폼을 Read해 **필드 구조·제목 포맷·라벨**을 확보한다:
    >   `bug.yml`(버그) · `feature.yml`(기능) · `etc.yml`(그 외 기타).
> - 브랜치 구성(8단계) 전에 `docs/conventions/branch-naming.md`를 Read해 prefix 규칙을 확보한다.

## 1. 사전 검증

막히면 안내만 출력 후 **종료**:

- **워킹 트리** — `git status --porcelain` 이 비어있지 않으면
  → `❌ 커밋되지 않은 변경사항이 있습니다. 커밋하거나 stash 후 다시 /issue를 실행해주세요.`
- **gh 인증** — `gh auth status` 실패 → `GitHub CLI 인증이 필요합니다. gh auth login 후 다시 시도해주세요.`

## 2. 설명 수집 & 유형 판정

- `$ARGUMENTS` 가 있으면 초기 설명으로 사용. 없으면 **한 번만** 자유 입력으로 질문:
  `어떤 이슈를 만들까요? 자유롭게 설명해주세요.`
- 설명을 분석해 유형을 **자동 결정**(묻지 않음, 매우 애매할 때만 1회 확인)하고 해당 .yml을 Read한다. 라벨은 폼의 `labels:`를 따르고, 제목 prefix는 아래 표대로 쓴다(`--title`을 명시로 넘기므로 폼의 `title:` 기본값과 달라도 됨):

  | 유형 | 템플릿 | 라벨 | 제목 |
    |---|---|---|---|
  | **Bug** (발생 중 문제 제보) | `bug.yml` | `Bug` | `[Fix]: <요약>` |
  | **Feature** (새 기능 제안) | `feature.yml` | `Feature` | `[Feat]: <요약>` |
  | **기타** (Docs·Chore·Refactor·Deploy·Test 등) | `etc.yml` | 없음 | `[<prefix>]: <요약>` (→ 3단계) |

## 3. (기타 경로만) 제목 prefix 결정

기타로 판정되면 설명 기반으로 **자동 결정**(묻지 않음): `Docs` / `Chore` / `Refactor` / `Deploy` / `Test` 중 하나.
→ 제목 `[<prefix>]: <요약>`. (Bug·Feature는 prefix 고정이라 이 단계 건너뜀)

## 4. 폼 필드 채우기

설명에 이미 있는 내용은 **건너뛰고**, 각 폼의 **필수 필드** 중 빠진 것만 AskUserQuestion(또는 자유 입력)으로 묻는다. 선택 필드(`기타`)는 관련 내용이 있을 때만 채운다.

- **bug.yml** — 버그 요약\*, 재현 방법\*, 실제 동작\*, 기타
- **feature.yml** — 기능 요약\*, 기타
- **etc.yml** — 작업 요약\*, 기타

(\* = 필수) Claude가 설명을 바탕으로 초안을 잡고, 개별로 잘게 묻지 않고 6단계에서 일괄 확인한다.

## 5. 중복 이슈 체크

제목에서 핵심 키워드 2~3개를 추출해 검색:
```sh
gh issue list --search "<키워드>" --state all --limit 5 --json number,title,state,url
```
결과가 있으면 표로 제시하고 AskUserQuestion(`계속 진행` / `취소`).

## 6. 본문 미리보기 & 확인

조립한 Markdown 본문을 보여주고 확정/수정 확인. 본문은 폼 필드를 `### <필드 라벨>` 형태로 렌더한다(이슈 폼 제출 시와 동일). 예(bug):

```markdown
### 버그 요약
<한 줄 요약>

### 재현 방법
1. ...
2. ...

### 실제 동작
<실제 동작>

### 기타
<로그·스크린샷·관련 이슈. 없으면 생략>
```

feature·etc는 `### 요약` + (선택)`### 기타` 형태로 더 단순하다.

## 7. 이슈 생성

```sh
gh issue create \
  --title "[<Prefix>]: <요약>" \
  --assignee @me \
  --body-file - <<'EOF'
<위에서 구성한 Markdown 본문>
EOF
```
- 라벨: Bug → `--label Bug`, Feature → `--label Feature`, **기타는 `--label` 생략**(폼에 라벨 미선언).
- 생성된 이슈 번호와 URL을 출력.

## 8. 브랜치 분기 (main 기준, 승인 없이 바로)

브랜치 네이밍은 **[`docs/conventions/branch-naming.md`](../../docs/conventions/branch-naming.md)**를 단일 출처로 따른다. 이름 조립:
- 그 문서의 prefix 가이드로 prefix 결정
- 포맷: `<prefix>/#<이슈번호>` (작업명 suffix 없음, 예: `feat/#4-user-create` 형태 사용 안 함)

체크아웃:
```sh
git fetch origin main
git checkout -b "<prefix>/#<이슈번호>" origin/main
```

요약 출력:
```
✅ 이슈 #<번호> 생성 완료 — <URL>
✅ 브랜치 <prefix>/#<번호> 체크아웃 완료 (main 기준)
```

## 취소

어느 단계에서든 취소 시 이슈·브랜치 생성 없이 종료.
