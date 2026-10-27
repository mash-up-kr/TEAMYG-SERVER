import { useState, type SyntheticEvent } from 'react'
import { LICENSE_TEXT_URL, loadLicenseText, OSS, SECTION_TITLE, sortedEntries, visibleSections, type OssEntry } from './licenses'
import type { Platform } from '../landing/ua'

type TextState = { kind: 'idle' } | { kind: 'loading' } | { kind: 'loaded'; text: string } | { kind: 'failed' }

function NewTabLink({ href, children }: { href: string; children: string }) {
  return (
    <a href={href} target="_blank" rel="noopener noreferrer">
      {children}
      <span className="sr-only"> 새 탭에서 열려요</span>
    </a>
  )
}

function Entry({ entry }: { entry: OssEntry }) {
  const [state, setState] = useState<TextState>({ kind: 'idle' })
  // 못 불러왔으면(배포로 청크 이름이 바뀐 경우 등) 다음에 펼칠 때 다시 부른다.
  const onToggle = (e: SyntheticEvent<HTMLDetailsElement>) => {
    if (!e.currentTarget.open || !entry.text) return
    if (state.kind !== 'idle' && state.kind !== 'failed') return
    setState({ kind: 'loading' })
    loadLicenseText(entry.text).then(
      (text) => setState({ kind: 'loaded', text }),
      () => setState({ kind: 'failed' }),
    )
  }
  return (
    <li>
      <details className="oss-item" onToggle={onToggle}>
        <summary>{entry.name}</summary>
        <dl>
          <dt>라이선스</dt>
          <dd>
            <NewTabLink href={LICENSE_TEXT_URL[entry.license]}>{entry.license}</NewTabLink>
          </dd>
          <dt>버전</dt>
          <dd>{entry.version}</dd>
          <dt>주소</dt>
          <dd>
            <NewTabLink href={entry.url}>{entry.url}</NewTabLink>
          </dd>
          {entry.modules && (
            <>
              <dt>모듈</dt>
              <dd>
                {entry.modules.map((m) => (
                  <code key={m}>{m}</code>
                ))}
              </dd>
            </>
          )}
        </dl>
        {entry.text ? (
          state.kind === 'loaded' ? (
            <pre className="oss-text">{state.text}</pre>
          ) : (
            <p className="oss-note">
              {state.kind === 'failed'
                ? '전문을 불러오지 못했어요. 다시 펼치거나 위 라이선스 링크에서 볼 수 있어요.'
                : '전문을 불러오는 중이에요.'}
            </p>
          )
        ) : (
          <p className="oss-note">약관 전문은 위 라이선스 링크에서 볼 수 있어요.</p>
        )}
      </details>
    </li>
  )
}

export default function Oss({ platform }: { platform: Platform }) {
  return (
    <>
      <header className="page-header">
        <p className="eyebrow">Parfait</p>
        <h1>오픈소스 라이선스</h1>
        <p className="meta">Parfait 앱에 쓰인 오픈소스 라이브러리와 글꼴이에요. 이름을 누르면 라이선스 전문이 보여요.</p>
      </header>
      <main className="content">
        {visibleSections(platform.os).map((section) => (
          <section key={section} aria-labelledby={`oss-${section}`}>
            <h2 id={`oss-${section}`}>{SECTION_TITLE[section]}</h2>
            <ul className="oss-list">
              {sortedEntries(OSS[section]).map((entry) => (
                <Entry key={entry.name} entry={entry} />
              ))}
            </ul>
          </section>
        ))}
      </main>
      <footer className="page-footer">디자인 토큰: ko-design-md toss.md (CC BY 4.0) · 글꼴: Pretendard (OFL-1.1)</footer>
    </>
  )
}
