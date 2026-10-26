import { CAMPAIGN_KEYS } from './ua'

// 랜딩 방문을 GA4 웹 스트림(Firebase 웹 앱 parfait-landing)으로 보낸다. 어드민은 이 파일을 쓰지 않는다.
// index.html의 인라인 스크립트는 하나여야 하므로(inlineScript.test.ts) gtag 스니펫을 여기서 만든다.
// Android 일반 브라우저는 인라인 스크립트가 스토어로 보낸다. intent 이동 뒤에도 페이지가 남아 이 코드가 도는지,
// 그래서 그 방문이 GA에 잡히는지는 실기기로 확인하지 않았다.
export const GA_MEASUREMENT_ID = 'G-CBZ3N541KR'

const GTAG_SRC = `https://www.googletagmanager.com/gtag/js?id=${GA_MEASUREMENT_ID}`

type GtagWindow = Window & { dataLayer?: unknown[]; gtag?: (...args: unknown[]) => void }

// GA에 보낼 주소. UTM이 아닌 쿼리(gclid, 링크에 붙은 다른 값)와 #fragment는 Google로 보내지 않는다.
// UTM 조각은 받은 그대로 둔다. 디코딩과 값 검사는 GA가 한다.
export function analyticsLocation(pageUrl: string): string {
  const noHash = pageUrl.split('#')[0]
  const q = noHash.indexOf('?')
  if (q < 0) return noHash
  const kept = noHash
    .slice(q + 1)
    .split('&')
    .filter((piece) => {
      const eq = piece.indexOf('=')
      return eq >= 0 && (CAMPAIGN_KEYS as readonly string[]).includes(piece.slice(0, eq))
    })
  return kept.length ? `${noHash.slice(0, q)}?${kept.join('&')}` : noHash.slice(0, q)
}

export function startAnalytics(win: GtagWindow = window, doc: Document = document): void {
  // 두 번 불리면(개발 서버의 HMR 등) page_view도 두 번 간다.
  if (doc.querySelector(`script[src="${GTAG_SRC}"]`)) return
  const dataLayer = (win.dataLayer = win.dataLayer || [])
  // gtag.js는 배열이 아니라 arguments 객체를 받아야 명령으로 읽는다.
  win.gtag = function gtag() {
    dataLayer.push(arguments)
  }
  win.gtag('js', new Date())
  win.gtag('config', GA_MEASUREMENT_ID, { page_location: analyticsLocation(win.location.href) })

  const script = doc.createElement('script')
  script.async = true
  script.src = GTAG_SRC
  doc.head.appendChild(script)
}
