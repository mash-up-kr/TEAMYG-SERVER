// 랜딩 방문을 GA4 웹 스트림(Firebase 웹 앱 parfait-landing)으로 보낸다. 어드민은 이 파일을 쓰지 않는다.
// index.html의 인라인 스크립트는 하나여야 하므로(inlineScript.test.ts) gtag 스니펫을 여기서 만든다.
// Android 일반 브라우저는 인라인 스크립트가 먼저 스토어로 보내서 여기까지 오지 않는다.
export const GA_MEASUREMENT_ID = 'G-CBZ3N541KR'

type GtagWindow = Window & { dataLayer?: unknown[]; gtag?: (...args: unknown[]) => void }

export function startAnalytics(win: GtagWindow = window, doc: Document = document): void {
  const dataLayer = (win.dataLayer = win.dataLayer || [])
  // gtag.js는 배열이 아니라 arguments 객체를 받아야 명령으로 읽는다.
  win.gtag = function gtag() {
    dataLayer.push(arguments)
  }
  win.gtag('js', new Date())
  // UTM은 page_location에서 GA가 직접 읽는다.
  win.gtag('config', GA_MEASUREMENT_ID)

  const script = doc.createElement('script')
  script.async = true
  script.src = `https://www.googletagmanager.com/gtag/js?id=${GA_MEASUREMENT_ID}`
  doc.head.appendChild(script)
}
