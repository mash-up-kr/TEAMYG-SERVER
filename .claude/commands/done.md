---
description: 현재 변경사항을 논리 단위로 쪼개 커밋한다. 메시지 컨벤션은 commit 스킬을 따른다
argument-hint: "[선택: 쪼개기 힌트]"
allowed-tools: Bash, AskUserQuestion, Read
disable-model-invocation: true
---

# /done — 커밋 마무리

현재 변경사항을 **논리 단위로 쪼개** 커밋한다. push·PR 생성은 범위 밖이다.
> **선행 로드**: 이 커맨드는 **[`docs/conventions/commit-message.md`](../../docs/conventions/commit-message.md)** 의 규칙을 따른다. 커밋 메시지 생성 단계(5)에 들어가기 전에 **반드시** 해당 파일을 Read해 최신 컨벤션·type 목록·예시를 확보한다. 커맨드 안에서 규칙을 재정의하지 않는다.

## 1. 상태 파악

```sh
git status --porcelain
git diff --stat && git diff --cached --stat
```

- 변경이 없으면 `✅ 커밋할 변경사항이 없습니다.` 출력 후 **종료**.
- 현재 브랜치(`git branch --show-current`)가 `main`/`develop`이면 **경고만 하고 진행**:
  `⚠️ 보호 브랜치(<브랜치>)에 직접 커밋합니다. 계속 진행합니다.`
- staged가 비어 있으면 `git add -A`, 이미 staged가 있으면 그대로 사용.

## 2. 논리 단위로 쪼개기

staged diff를 관심사별로 그룹핑한다 (기능↔리팩토링, 코드↔테스트, 코드↔문서, 의존성↔기능, 모듈·패키지별 경계). **한 커밋 = 한 목적.**
`$ARGUMENTS` 가 넘어오면 쪼개기 힌트로 참고한다.

- 하나의 의도로 묶이면 **단일 커밋**으로 바로 진행.
- 여러 단위면 계획을 보여주고 **AskUserQuestion으로 한 번만** 확인:

```
분할 계획 (N개)
[1/N] feat: 로그인 API 개발
- app/src/main/.../LoginRepository.kt
- app/src/main/.../LoginUseCase.kt

[2/N] test: 로그인 UseCase 테스트 추가
- app/src/test/.../LoginUseCaseTest.kt

[3/N] chore: OkHttp 4.12 업그레이드
- gradle/libs.versions.toml
```

AskUserQuestion 선택지: `이대로 진행`(기본) / `단일 커밋으로 합치기` / `취소`.

승인 후에는 단위마다 다시 묻지 않고 순차적으로 커밋한다.

## 3. 단위별 커밋

각 단위에 대해 반복:

### 3-1. 메시지 생성
변경 내용을 기반으로 Conventional Commits 한국어 메시지 생성:
- `<type>: <한국어 제목>` (scope 없음, 마침표 없음)
- 필요 시 한 줄 공백 후 한국어 본문 (왜 바꿨는지 중심)
- 본문은 고정 글자 수로 임의로 줄바꿈하지 않는다. 줄바꿈은 문장이 끝나는 지점에서만 하고, 한 문장 안에서 강제로 끊지 않는다 — 중간에서 끊으면 GitHub·`git log` 등에서 어색하게 읽힌다
- `Co-Authored-By` 꼬리표 **붙이지 않음**

### 3-2. 커밋 실행

```sh
# 해당 단위의 파일만 staged 유지하도록 reset 후 재-add
git reset HEAD
git add <files-of-this-unit>

# 커밋 (multiline 메시지는 -F 사용)
TMP=$(mktemp -t commit-msg)
cat > "$TMP" <<'EOF'
<type>: <한국어 제목>

<본문 (있을 때만)>
EOF

git commit -F "$TMP"
rm -f "$TMP"
```

- 메시지의 type·scope·본문 규칙은 **[`docs/conventions/commit-message.md`](../../docs/conventions/commit-message.md)** 을 따른다.
- pre-commit hook이 실패하면 실패 이유를 그대로 출력하고 **그 단위에서 멈춘다** (앞서 만든 커밋은 유지). 정리 후 `/done` 재실행을 안내.

## 4. 결과

모든 단위 처리 후 최종 요약 출력:

```sh
echo "✅ 커밋 완료"
git log --oneline -<N>
echo "현재 브랜치: $(git branch --show-current)"
```

로 생성된 커밋을 보여준다. 스킵·취소된 단위가 있으면 함께 알린다.
