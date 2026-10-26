import { describe, expect, it } from 'vitest'
import { CAMPAIGN_CASES } from './__fixtures__/campaignUrls'
import {
  APP_STORE_URL,
  campaignReferrer,
  detectPlatform,
  externalBrowserUrl,
  landingView,
  playIntentUrl,
  playWebUrl,
  PLAY_WEB_URL,
} from './ua'

const IPHONE =
  'Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1'
const MAC =
  'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15'
const ANDROID =
  'Mozilla/5.0 (Linux; Android 14; SM-S918N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36'
const WINDOWS =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36'

describe('detectPlatform', () => {
  it('iPhone Safari는 ios', () => {
    expect(detectPlatform(IPHONE, 5)).toEqual({ os: 'ios', inApp: false, kakao: false })
  })

  it('터치 지원 Macintosh UA(iPadOS)는 ios, 터치 없으면 other', () => {
    expect(detectPlatform(MAC, 5).os).toBe('ios')
    expect(detectPlatform(MAC, 0).os).toBe('other')
  })

  it('Android Chrome은 android, 인앱 아님', () => {
    expect(detectPlatform(ANDROID, 5)).toMatchObject({ os: 'android', inApp: false })
  })

  it('카카오톡 인앱', () => {
    expect(detectPlatform(`${ANDROID} KAKAOTALK/10.0.0`, 5)).toMatchObject({
      os: 'android',
      inApp: true,
      kakao: true,
    })
  })

  it.each(['Instagram 300.0', 'NAVER(inapp; search)', 'FBAV/450.0', 'Line/13.0', 'Threads 300'])(
    '%s는 인앱',
    (token) => {
      expect(detectPlatform(`${ANDROID} ${token}`, 5)).toMatchObject({ inApp: true, kakao: false })
    },
  )

  it('Windows Chrome은 other', () => {
    expect(detectPlatform(WINDOWS, 0).os).toBe('other')
  })
})

describe('urls', () => {
  it('playIntentUrl', () => {
    expect(playIntentUrl()).toBe(
      'intent://details?id=com.teamyg.parfait#Intent;scheme=market;package=com.android.vending;' +
        `S.browser_fallback_url=${encodeURIComponent(PLAY_WEB_URL)};end`,
    )
  })

  it('externalBrowserUrl은 fragment를 떼고 Chrome intent를 만든다', () => {
    expect(externalBrowserUrl('https://a.b/c?x=1#frag', false)).toBe(
      'intent://a.b/c?x=1#Intent;scheme=https;package=com.android.chrome;S.browser_fallback_url=https%3A%2F%2Fa.b%2Fc%3Fx%3D1;end',
    )
  })

  it('카카오는 openExternal', () => {
    expect(externalBrowserUrl('https://a.b/c', true)).toBe(
      'kakaotalk://web/openExternal?url=https%3A%2F%2Fa.b%2Fc',
    )
  })
})

const R = 'utm_source=instagram&utm_medium=social&utm_campaign=x'

describe('campaignReferrer', () => {
  it.each(CAMPAIGN_CASES)('%s', (_name, pageUrl, referrer) => {
    expect(campaignReferrer(pageUrl)).toBe(referrer)
  })
})
describe('play urls with a referrer', () => {
  it('keeps the old strings without a referrer', () => {
    expect(PLAY_WEB_URL).toBe('https://play.google.com/store/apps/details?id=com.teamyg.parfait')
    expect(playWebUrl()).toBe(PLAY_WEB_URL)
    expect(playWebUrl('')).toBe(PLAY_WEB_URL)
    expect(playIntentUrl('')).toBe(playIntentUrl())
    expect(playIntentUrl()).toBe(
      'intent://details?id=com.teamyg.parfait#Intent;scheme=market;package=com.android.vending;' +
        'S.browser_fallback_url=https%3A%2F%2Fplay.google.com%2Fstore%2Fapps%2Fdetails%3Fid%3Dcom.teamyg.parfait;end',
    )
  })
  it('adds the encoded referrer to the web url', () => {
    expect(playWebUrl(R)).toBe(
      'https://play.google.com/store/apps/details?id=com.teamyg.parfait&referrer=utm_source%3Dinstagram%26utm_medium%3Dsocial%26utm_campaign%3Dx',
    )
  })
  it('adds the referrer to the intent and to its fallback url', () => {
    expect(playIntentUrl(R)).toBe(
      'intent://details?id=com.teamyg.parfait&referrer=utm_source%3Dinstagram%26utm_medium%3Dsocial%26utm_campaign%3Dx' +
        '#Intent;scheme=market;package=com.android.vending;S.browser_fallback_url=' +
        'https%3A%2F%2Fplay.google.com%2Fstore%2Fapps%2Fdetails%3Fid%3Dcom.teamyg.parfait%26referrer%3Dutm_source%253Dinstagram%2526utm_medium%253Dsocial%2526utm_campaign%253Dx;end',
    )
  })
})

const UK = 'https://x/y?utm_source=kakaotalk&utm_medium=social&utm_campaign=c#f'
const RK = 'utm_source=kakaotalk&utm_medium=social&utm_campaign=c'
const store = (href: string) => ({ id: 'store', label: 'Google Play에서 다운로드', href, variant: 'primary' })
const appstore = { id: 'appstore', label: 'App Store에서 다운로드', href: APP_STORE_URL, variant: 'primary' }

describe('landingView', () => {
  it('sends a plain Android browser to the intent with the referrer', () => {
    expect(landingView({ os: 'android', inApp: false, kakao: false }, UK)).toEqual({
      autoRedirect: playIntentUrl(RK), noRedirectReason: null,
      buttons: [store(playIntentUrl(RK))], showInAppHint: false, referrer: RK,
    })
  })
  it('keeps an Android in-app browser on the page with both buttons and the hint', () => {
    expect(landingView({ os: 'android', inApp: true, kakao: true }, UK)).toEqual({
      autoRedirect: null, noRedirectReason: 'in-app',
      buttons: [
        store(playIntentUrl(RK)),
        { id: 'external', label: '외부 브라우저로 열기', href: externalBrowserUrl(UK, true), variant: 'secondary' },
      ],
      showInAppHint: true, referrer: RK,
    })
  })
  it('shows only the App Store on iOS, in-app or not, and passes nothing to Play', () => {
    for (const inApp of [false, true]) {
      expect(landingView({ os: 'ios', inApp, kakao: inApp }, UK)).toEqual({
        autoRedirect: null, noRedirectReason: 'not-android', buttons: [appstore], showInAppHint: false, referrer: '',
      })
    }
  })
  it('shows both stores elsewhere, with the referrer on the Play web url', () => {
    expect(landingView({ os: 'other', inApp: false, kakao: false }, UK)).toEqual({
      autoRedirect: null, noRedirectReason: 'not-android',
      buttons: [store(playWebUrl(RK)), appstore], showInAppHint: false, referrer: RK,
    })
  })
  it('is unchanged without campaign params', () => {
    const v = landingView({ os: 'android', inApp: false, kakao: false }, 'https://x/y')
    expect(v.autoRedirect).toBe(playIntentUrl())
    expect(v.referrer).toBe('')
    expect(landingView({ os: 'other', inApp: false, kakao: false }, 'https://x/y').buttons[0].href).toBe(PLAY_WEB_URL)
  })
})
