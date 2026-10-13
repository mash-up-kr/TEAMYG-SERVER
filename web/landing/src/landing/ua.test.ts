import { describe, expect, it } from 'vitest'
import { detectPlatform, externalBrowserUrl, playIntentUrl, PLAY_WEB_URL } from './ua'

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
