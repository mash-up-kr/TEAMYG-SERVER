export type Platform = { os: 'ios' | 'android' | 'other'; inApp: boolean; kakao: boolean }

export const PACKAGE = 'com.teamyg.parfait'
export const PLAY_WEB_URL = `https://play.google.com/store/apps/details?id=${PACKAGE}`
export const APP_STORE_URL =
  'https://apps.apple.com/kr/app/%ED%8C%8C%EB%A5%B4%ED%8E%98-parfait-%EC%82%AC%EC%A7%84-%EA%B3%B5%EC%9C%A0-%EC%BA%94%EB%B2%84%EC%8A%A4-sns/id6806914364'

// 아래 정규식과 playIntentUrl 조립은 index.html <head>의 인라인 스크립트와 같아야 한다.
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

// Play 스토어 앱을 직접 여는 intent. 스토어 앱이 없으면 Play 웹으로 폴백한다.
export function playIntentUrl(): string {
  return (
    `intent://details?id=${PACKAGE}` +
    '#Intent;scheme=market;package=com.android.vending;' +
    `S.browser_fallback_url=${encodeURIComponent(PLAY_WEB_URL)};end`
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
