// 앱에 들어간 오픈소스 목록. 손으로 옮겨 적은 값이라 앱 의존성이 바뀌면 같이 고친다.
// Android: TEAMYG-Android gradle/libs.versions.toml (main 954e592). 테스트·빌드 도구와 전이 의존성은 넣지 않았다.
//   예외로 OkHttp 아티팩트에 들어 있는 Public Suffix List는 넣었다.
// iOS: TEAMYG-iOS Parfait.xcodeproj/.../swiftpm/Package.resolved (main 627a1b8). 전이 의존성까지 고정된 목록이다.
// 글꼴: SUIT는 두 앱 모두, LINE Seed JP·TW는 텍스트 토핑과 함께 들어왔다(Android develop).
// 라이선스는 각 프로젝트의 GitHub 라이선스나 Maven POM의 <license>를 따랐다.

// 전문은 펼칠 때 불러온다. 목록을 처음 그릴 때 전문 40여 개를 받지 않으려고.
const TEXTS = import.meta.glob<string>('./texts/*.txt', { query: '?raw', import: 'default' })

export function loadLicenseText(key: string): Promise<string> {
  const load = TEXTS[`./texts/${key}.txt`]
  if (!load) return Promise.reject(new Error(`라이선스 전문이 없다: ${key}`))
  return load()
}

export type Section = 'font' | 'android' | 'ios'

export type LicenseKind =
  | 'Apache-2.0'
  | 'MIT'
  | 'BSD-3-Clause'
  | 'Zlib'
  | 'MPL-2.0'
  | 'OFL-1.1'
  | 'Android Software Development Kit License'
  | 'ML Kit Terms of Service'

export type OssEntry = {
  name: string
  version: string
  license: LicenseKind
  url: string
  /** texts/<이름>.txt. 프로젝트 LICENSE 파일을 그대로 옮겼고, 표준 Apache 전문과 같으면 apache-2.0을 함께 쓴다.
   *  NOTICE 파일이 있는 Apache 프로젝트는 그 내용을 전문 뒤에 붙였다(Apache-2.0 §4(d)).
   *  없으면(Google 약관) 링크만 보인다. */
  text?: string
  /** 함께 묶은 모듈(아티팩트, SPM 제품). 하나뿐이면 비운다. */
  modules?: string[]
}

export const LICENSE_TEXT_URL: Record<LicenseKind, string> = {
  'Apache-2.0': 'https://www.apache.org/licenses/LICENSE-2.0',
  MIT: 'https://opensource.org/license/mit',
  'BSD-3-Clause': 'https://opensource.org/license/bsd-3-clause',
  Zlib: 'https://opensource.org/license/zlib',
  'MPL-2.0': 'https://www.mozilla.org/MPL/2.0/',
  'OFL-1.1': 'https://openfontlicense.org/open-font-license-official-text/',
  'Android Software Development Kit License': 'https://developer.android.com/studio/terms',
  'ML Kit Terms of Service': 'https://developers.google.com/ml-kit/terms',
}

export const SECTION_TITLE: Record<Section, string> = { font: 'Font', android: 'Android', ios: 'iOS' }

export const OSS: Record<Section, OssEntry[]> = {
  font: [
    { name: 'SUIT', version: '2.040', text: 'suit', license: 'OFL-1.1', url: 'https://github.com/sun-typeface/SUIT' },
    { name: 'LINE Seed JP', version: 'Regular', text: 'line-seed-jp', license: 'OFL-1.1', url: 'https://seed.line.me/index_jp.html' },
    { name: 'LINE Seed TW', version: 'Regular', text: 'line-seed-tw', license: 'OFL-1.1', url: 'https://seed.line.me/index_tw.html' },
  ],
  android: [
    { name: 'AndroidX Activity', version: '1.13.0', text: 'apache-2.0', license: 'Apache-2.0', url: 'https://developer.android.com/jetpack/androidx/releases/activity', modules: ['androidx.activity:activity-compose'] },
    { name: 'AndroidX AppCompat', version: '1.7.1', text: 'apache-2.0', license: 'Apache-2.0', url: 'https://developer.android.com/jetpack/androidx/releases/appcompat' },
    {
      name: 'AndroidX CameraX',
      version: '1.6.1',
      text: 'apache-2.0',
      license: 'Apache-2.0',
      url: 'https://developer.android.com/jetpack/androidx/releases/camera',
      modules: ['camera-core', 'camera-camera2', 'camera-lifecycle', 'camera-view'],
    },
    { name: 'AndroidX Core', version: '1.19.0', text: 'apache-2.0', license: 'Apache-2.0', url: 'https://developer.android.com/jetpack/androidx/releases/core', modules: ['androidx.core:core-ktx'] },
    { name: 'AndroidX DataStore', version: '1.2.1', text: 'apache-2.0', license: 'Apache-2.0', url: 'https://developer.android.com/jetpack/androidx/releases/datastore', modules: ['datastore-preferences'] },
    { name: 'AndroidX ExifInterface', version: '1.4.2', text: 'apache-2.0', license: 'Apache-2.0', url: 'https://developer.android.com/jetpack/androidx/releases/exifinterface' },
    {
      name: 'AndroidX Hilt',
      version: '1.4.0',
      text: 'apache-2.0',
      license: 'Apache-2.0',
      url: 'https://developer.android.com/jetpack/androidx/releases/hilt',
      modules: ['hilt-navigation-compose', 'hilt-lifecycle-viewmodel-compose'],
    },
    {
      name: 'AndroidX Lifecycle',
      version: '2.11.0',
      text: 'apache-2.0',
      license: 'Apache-2.0',
      url: 'https://developer.android.com/jetpack/androidx/releases/lifecycle',
      modules: ['lifecycle-runtime-ktx', 'lifecycle-viewmodel-compose', 'lifecycle-viewmodel-navigation3'],
    },
    {
      name: 'AndroidX Navigation 3',
      version: '1.2.0-alpha07',
      text: 'apache-2.0',
      license: 'Apache-2.0',
      url: 'https://developer.android.com/jetpack/androidx/releases/navigation3',
      modules: ['navigation3-runtime', 'navigation3-ui'],
    },
    { name: 'Coil', version: '3.5.0', text: 'coil', license: 'Apache-2.0', url: 'https://github.com/coil-kt/coil', modules: ['coil-compose', 'coil-network-okhttp'] },
    { name: 'Dagger Hilt', version: '2.60.1', text: 'apache-2.0', license: 'Apache-2.0', url: 'https://github.com/google/dagger', modules: ['hilt-android'] },
    {
      name: 'Firebase Android SDK',
      version: 'BoM 34.18.0',
      text: 'apache-2.0',
      license: 'Apache-2.0',
      url: 'https://github.com/firebase/firebase-android-sdk',
      modules: ['firebase-crashlytics', 'firebase-crashlytics-ndk', 'firebase-messaging'],
    },
    // firebase-analytics의 POM은 Apache가 아니라 Android SDK License다.
    {
      name: 'Firebase Analytics',
      version: '23.2.0 (BoM 34.18.0)',
      license: 'Android Software Development Kit License',
      url: 'https://firebase.google.com/docs/analytics',
      modules: ['firebase-analytics'],
    },
    {
      name: 'Google Play Install Referrer',
      version: '2.2',
      license: 'Android Software Development Kit License',
      url: 'https://developer.android.com/google/play/installreferrer',
    },
    { name: 'Haze', version: '1.7.2', text: 'apache-2.0', license: 'Apache-2.0', url: 'https://github.com/chrisbanes/haze' },
    { name: 'javax.inject', version: '1', text: 'apache-2.0', license: 'Apache-2.0', url: 'https://github.com/javax-inject/javax-inject' },
    {
      name: 'Jetpack Compose',
      version: 'BoM 2026.08.00',
      text: 'apache-2.0',
      license: 'Apache-2.0',
      url: 'https://developer.android.com/jetpack/androidx/releases/compose',
      modules: ['compose-ui', 'compose-ui-graphics', 'compose-ui-tooling-preview', 'compose-material3'],
    },
    { name: 'Kakao SDK', version: '2.24.0', text: 'apache-2.0', license: 'Apache-2.0', url: 'https://developers.kakao.com/docs/ko/android/getting-started', modules: ['v2-user'] },
    { name: 'Kermit', version: '2.1.0', text: 'kermit', license: 'Apache-2.0', url: 'https://github.com/touchlab/Kermit' },
    { name: 'Kotlin Standard Library', version: '2.4.10', text: 'kotlin-stdlib', license: 'Apache-2.0', url: 'https://github.com/JetBrains/kotlin' },
    {
      name: 'kotlinx.coroutines',
      version: '1.11.0',
      text: 'kotlinx-coroutines',
      license: 'Apache-2.0',
      url: 'https://github.com/Kotlin/kotlinx.coroutines',
      modules: ['kotlinx-coroutines-core', 'kotlinx-coroutines-android'],
    },
    { name: 'kotlinx-datetime', version: '0.8.0', text: 'apache-2.0', license: 'Apache-2.0', url: 'https://github.com/Kotlin/kotlinx-datetime' },
    { name: 'kotlinx.serialization', version: '1.11.0', text: 'kotlinx-serialization', license: 'Apache-2.0', url: 'https://github.com/Kotlin/kotlinx.serialization', modules: ['kotlinx-serialization-json'] },
    { name: 'Lottie for Android', version: '6.7.1', text: 'apache-2.0', license: 'Apache-2.0', url: 'https://github.com/airbnb/lottie-android', modules: ['lottie-compose'] },
    {
      name: 'ML Kit Subject Segmentation',
      version: '16.0.0-beta1',
      license: 'ML Kit Terms of Service',
      url: 'https://developers.google.com/ml-kit/vision/subject-segmentation',
    },
    { name: 'OkHttp', version: '5.5.0', text: 'apache-2.0', license: 'Apache-2.0', url: 'https://github.com/square/okhttp', modules: ['okhttp', 'logging-interceptor'] },
    // OkHttp Android 아티팩트에 publicsuffix.org 목록(PublicSuffixDatabase.list)이 들어 있다.
    {
      name: 'Public Suffix List',
      version: 'OkHttp 5.5.0에 포함',
      text: 'mpl-2.0',
      license: 'MPL-2.0',
      url: 'https://publicsuffix.org/',
    },
    { name: 'Retrofit', version: '3.0.0', text: 'apache-2.0', license: 'Apache-2.0', url: 'https://github.com/square/retrofit', modules: ['retrofit', 'converter-kotlinx-serialization'] },
  ],
  ios: [
    { name: 'Abseil', version: '1.2024072200.0', text: 'apache-2.0', license: 'Apache-2.0', url: 'https://github.com/google/abseil-cpp-binary' },
    { name: 'Alamofire', version: '5.12.0', text: 'alamofire', license: 'MIT', url: 'https://github.com/Alamofire/Alamofire' },
    { name: 'App Check', version: '11.3.1', text: 'apache-2.0', license: 'Apache-2.0', url: 'https://github.com/google/app-check' },
    {
      name: 'Firebase iOS SDK',
      version: '12.18.0',
      text: 'apache-2.0',
      license: 'Apache-2.0',
      url: 'https://github.com/firebase/firebase-ios-sdk',
      modules: ['FirebaseAnalytics', 'FirebaseCrashlytics', 'FirebaseMessaging'],
    },
    {
      name: 'Google Ads On-Device Conversion SDK',
      version: '3.6.1',
      text: 'apache-2.0',
      license: 'Apache-2.0',
      url: 'https://github.com/googleads/google-ads-on-device-conversion-ios-sdk',
    },
    { name: 'GoogleAppMeasurement', version: '12.18.0', text: 'apache-2.0', license: 'Apache-2.0', url: 'https://github.com/google/GoogleAppMeasurement' },
    { name: 'GoogleDataTransport', version: '10.1.1', text: 'apache-2.0', license: 'Apache-2.0', url: 'https://github.com/google/GoogleDataTransport' },
    { name: 'GoogleUtilities', version: '8.1.3', text: 'google-utilities', license: 'Apache-2.0', url: 'https://github.com/google/GoogleUtilities' },
    { name: 'gRPC', version: '1.69.1', text: 'apache-2.0', license: 'Apache-2.0', url: 'https://github.com/google/grpc-binary' },
    { name: 'GTMSessionFetcher', version: '5.3.1', text: 'apache-2.0', license: 'Apache-2.0', url: 'https://github.com/google/gtm-session-fetcher' },
    { name: 'Interop for Google SDKs', version: '101.0.0', text: 'apache-2.0', license: 'Apache-2.0', url: 'https://github.com/google/interop-ios-for-google-sdks' },
    {
      name: 'Kakao SDK for iOS',
      version: '2.28.0',
      text: 'apache-2.0',
      license: 'Apache-2.0',
      url: 'https://github.com/kakao/kakao-ios-sdk',
      modules: ['KakaoSDKAuth', 'KakaoSDKCommon', 'KakaoSDKUser'],
    },
    { name: 'LevelDB', version: '1.22.5', text: 'leveldb', license: 'BSD-3-Clause', url: 'https://github.com/firebase/leveldb' },
    { name: 'Lottie for iOS', version: '4.6.1', text: 'lottie-ios', license: 'Apache-2.0', url: 'https://github.com/airbnb/lottie-ios' },
    { name: 'nanopb', version: '2.30910.1', text: 'nanopb', license: 'Zlib', url: 'https://github.com/firebase/nanopb' },
    { name: 'Promises', version: '2.4.1', text: 'apache-2.0', license: 'Apache-2.0', url: 'https://github.com/google/promises' },
  ],
}

// Font는 늘 보인다. Android 기기는 Android만, iOS 기기는 iOS만, 그 밖에는 둘 다.
export function visibleSections(os: 'ios' | 'android' | 'other'): Section[] {
  if (os === 'android') return ['font', 'android']
  if (os === 'ios') return ['font', 'ios']
  return ['font', 'android', 'ios']
}

// 이름의 문자열 순. 대소문자는 가리지 않는다(kotlinx가 Kotlin 옆에 온다).
export function sortedEntries(entries: OssEntry[]): OssEntry[] {
  return [...entries].sort((a, b) => a.name.localeCompare(b.name, 'en', { sensitivity: 'base' }))
}
