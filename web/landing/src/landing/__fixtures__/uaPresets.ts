// 원본 레포 src/admin/pages/devtools/ua-tester/config.ts의 복사본이다. 어드민은 가져오지 않아서 테스트가 이 파일을 쓴다.
export type UaPreset = { id: string; label: string; ua: string; touch: boolean }

export const UA_PRESETS: readonly UaPreset[] = [
  {
    id: 'iphone-safari',
    label: 'iPhone Safari',
    ua: 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1',
    touch: true,
  },
  {
    id: 'ipad',
    label: 'iPad',
    ua: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15',
    touch: true,
  },
  {
    id: 'android-chrome',
    label: 'Android Chrome',
    ua: 'Mozilla/5.0 (Linux; Android 14; SM-S918N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36',
    touch: true,
  },
  {
    id: 'instagram-android',
    label: '인스타그램 인앱',
    ua: 'Mozilla/5.0 (Linux; Android 14; SM-S918N; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/124.0.0.0 Mobile Safari/537.36 Instagram 330.0.0.40.92 Android',
    touch: true,
  },
  {
    id: 'kakaotalk-android',
    label: '카카오톡 인앱 (Android)',
    ua: 'Mozilla/5.0 (Linux; Android 14; SM-S918N; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/124.0.0.0 Mobile Safari/537.36 KAKAOTALK/10.0.0',
    touch: true,
  },
  {
    id: 'kakaotalk-ios',
    label: '카카오톡 인앱 (iOS)',
    ua: 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 KAKAOTALK/10.0.0',
    touch: true,
  },
  {
    id: 'naver-android',
    label: '네이버 앱',
    ua: 'Mozilla/5.0 (Linux; Android 14; SM-S918N; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/124.0.0.0 Mobile Safari/537.36 NAVER(inapp; search; 2000; 12.8.5)',
    touch: true,
  },
  {
    id: 'desktop',
    label: '데스크톱',
    ua: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    touch: false,
  },
]
