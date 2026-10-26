// DOM을 만지지 않는 순수 함수와 상수만 둔다. 어드민도 이 파일을 import한다.
export type Platform = { os: 'ios' | 'android' | 'other'; inApp: boolean; kakao: boolean }

export const PACKAGE = 'com.teamyg.parfait'
export const APP_STORE_URL =
  'https://apps.apple.com/kr/app/%ED%8C%8C%EB%A5%B4%ED%8E%98-parfait-%EC%82%AC%EC%A7%84-%EA%B3%B5%EC%9C%A0-%EC%BA%94%EB%B2%84%EC%8A%A4-sns/id6806914364'

// index.html <head>의 인라인 스크립트는 landingView().autoRedirect와 같아야 한다.
// src/landing/inlineScript.test.ts가 그것을 확인한다.
const IN_APP = /Instagram|FBAN|FBAV|FB_IAB|KAKAOTALK|NAVER|Line\/|Threads/i

export function detectPlatform(ua: string, maxTouchPoints: number): Platform {
  const kakao = /KAKAOTALK/i.test(ua)
  const inApp = IN_APP.test(ua)
  // iPadOS는 데스크톱 Safari UA(Macintosh)를 쓰므로 터치 지원 여부로 구분한다.
  const isIOS = /iPhone|iPad|iPod/i.test(ua) || (/Macintosh/i.test(ua) && maxTouchPoints > 1)
  if (isIOS) return { os: 'ios', inApp, kakao }
  if (/Android/i.test(ua)) return { os: 'android', inApp, kakao }
  return { os: 'other', inApp, kakao }
}

export const CAMPAIGN_KEYS = ['utm_source', 'utm_medium', 'utm_campaign', 'utm_content', 'utm_term', 'utm_id'] as const

const CAMPAIGN_VALUE = /^[A-Za-z0-9._~-]{1,100}$/

// 페이지 주소에서 캠페인 값을 뽑는다. 디코딩하지 않고 URL/URLSearchParams도 쓰지 않는다.
// index.html 인라인 스크립트가 같은 절차를 ES5로 되풀이한다. 넘길 것이 없으면 ''.
export function campaignReferrer(pageUrl: string): string {
  const noHash = pageUrl.split('#')[0]
  const q = noHash.indexOf('?')
  if (q < 0) return ''
  const found: Record<string, string | null> = {}
  for (const piece of noHash.slice(q + 1).split('&')) {
    const eq = piece.indexOf('=')
    if (eq < 0) continue
    const key = piece.slice(0, eq)
    if (!(CAMPAIGN_KEYS as readonly string[]).includes(key)) continue
    // 같은 key는 첫 조각만 본다. 틀렸으면 그 key는 없는 것이다.
    if (Object.prototype.hasOwnProperty.call(found, key)) continue
    const value = piece.slice(eq + 1)
    found[key] = CAMPAIGN_VALUE.test(value) ? value : null
  }
  if (!found.utm_source) return ''
  const parts: string[] = []
  for (const key of CAMPAIGN_KEYS) {
    const value = found[key]
    if (value) parts.push(`${key}=${value}`)
  }
  return parts.join('&')
}

export function playWebUrl(referrer?: string): string {
  const base = `https://play.google.com/store/apps/details?id=${PACKAGE}`
  return referrer ? `${base}&referrer=${encodeURIComponent(referrer)}` : base
}

export const PLAY_WEB_URL = playWebUrl()

// Play 스토어 앱을 직접 여는 intent. 스토어 앱이 없으면 Play 웹으로 폴백한다.
export function playIntentUrl(referrer?: string): string {
  const ref = referrer ? `&referrer=${encodeURIComponent(referrer)}` : ''
  return (
    `intent://details?id=${PACKAGE}${ref}` +
    '#Intent;scheme=market;package=com.android.vending;' +
    `S.browser_fallback_url=${encodeURIComponent(playWebUrl(referrer))};end`
  )
}

// 인앱 브라우저에서 외부 브라우저로 여는 URL. #fragment는 intent의 #Intent 구분자와 섞이므로 뗀다.
export function externalBrowserUrl(pageUrl: string, kakao: boolean): string {
  const url = pageUrl.split('#')[0]
  if (kakao) return `kakaotalk://web/openExternal?url=${encodeURIComponent(url)}`
  const here = url.replace(/^https?:\/\//, '')
  return (
    `intent://${here}#Intent;scheme=https;package=com.android.chrome;` +
    `S.browser_fallback_url=${encodeURIComponent(url)};end`
  )
}

export type LandingButton = {
  id: 'store' | 'appstore' | 'external'
  label: string
  href: string
  variant: 'primary' | 'secondary'
}
export type LandingView = {
  /** 인라인 스크립트가 페이지를 그리기 전에 보내는 주소. 보내지 않으면 null. */
  autoRedirect: string | null
  /** 자동 이동을 하지 않는 이유. 하면 null. */
  noRedirectReason: 'in-app' | 'not-android' | null
  buttons: LandingButton[]
  /** 인앱 브라우저 안내 문구를 보이는지. */
  showInAppHint: boolean
  /** Play로 넘기는 캠페인 문자열. 없으면 ''. */
  referrer: string
}

// 랜딩이 어떤 버튼을 어느 주소로 보일지의 판정. Landing.tsx와 어드민 분기 테스트가 함께 쓴다.
export function landingView(platform: Platform, pageUrl: string): LandingView {
  const { os, inApp, kakao } = platform
  const referrer = os === 'ios' ? '' : campaignReferrer(pageUrl)
  const appstore: LandingButton = {
    id: 'appstore',
    label: 'App Store에서 다운로드',
    href: APP_STORE_URL,
    variant: 'primary',
  }
  const store = (href: string): LandingButton => ({
    id: 'store',
    label: 'Google Play에서 다운로드',
    href,
    variant: 'primary',
  })

  if (os === 'ios') {
    return { autoRedirect: null, noRedirectReason: 'not-android', buttons: [appstore], showInAppHint: false, referrer }
  }
  if (os === 'other') {
    return {
      autoRedirect: null,
      noRedirectReason: 'not-android',
      buttons: [store(playWebUrl(referrer)), appstore],
      showInAppHint: false,
      referrer,
    }
  }
  const intent = playIntentUrl(referrer)
  if (!inApp) {
    return { autoRedirect: intent, noRedirectReason: null, buttons: [store(intent)], showInAppHint: false, referrer }
  }
  return {
    autoRedirect: null,
    noRedirectReason: 'in-app',
    buttons: [
      store(intent),
      { id: 'external', label: '외부 브라우저로 열기', href: externalBrowserUrl(pageUrl, kakao), variant: 'secondary' },
    ],
    showInAppHint: true,
    referrer,
  }
}
