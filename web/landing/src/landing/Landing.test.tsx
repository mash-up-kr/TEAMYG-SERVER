import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import Landing from './Landing'
import { externalBrowserUrl, playIntentUrl, playWebUrl, type Platform } from './ua'

const view = (platform: Platform, pageUrl = 'https://x/y') =>
  render(<Landing platform={platform} pageUrl={pageUrl} />)

const UK = 'https://x/y?utm_source=kakaotalk&utm_medium=social&utm_campaign=c#f'
const RK = 'utm_source=kakaotalk&utm_medium=social&utm_campaign=c'

describe('Landing', () => {
  it('ios는 App Store 버튼만 보인다', () => {
    view({ os: 'ios', inApp: false, kakao: false })
    expect(screen.getByText('App Store에서 다운로드')).toBeInTheDocument()
    expect(screen.queryByText('Google Play에서 다운로드')).not.toBeInTheDocument()
  })

  it('other는 두 버튼이 보이고 외부 브라우저 안내는 없다', () => {
    view({ os: 'other', inApp: false, kakao: false })
    expect(screen.getByText('App Store에서 다운로드')).toBeInTheDocument()
    expect(screen.getByText('Google Play에서 다운로드')).toBeInTheDocument()
    expect(screen.queryByText('외부 브라우저로 열기')).not.toBeInTheDocument()
    expect(screen.queryByText('⋮')).not.toBeInTheDocument()
  })

  it('android 인앱은 intent 링크, 외부 브라우저 버튼, 안내 문구를 보인다', () => {
    view({ os: 'android', inApp: true, kakao: false })
    expect(screen.getByText('Google Play에서 다운로드')).toHaveAttribute('href', playIntentUrl())
    expect(screen.getByText('외부 브라우저로 열기')).toBeInTheDocument()
    expect(screen.getByText('⋮')).toBeInTheDocument()
    expect(screen.queryByText('App Store에서 다운로드')).not.toBeInTheDocument()
  })

  it('android 인앱은 캠페인 referrer를 Play 링크에 싣고 외부 브라우저 링크는 쿼리째 넘긴다', () => {
    view({ os: 'android', inApp: true, kakao: false }, UK)
    expect(screen.getByText('Google Play에서 다운로드')).toHaveAttribute('href', playIntentUrl(RK))
    expect(screen.getByText('외부 브라우저로 열기')).toHaveAttribute('href', externalBrowserUrl(UK, false))
  })

  it('other는 Play 웹 주소에 referrer를 싣는다', () => {
    view({ os: 'other', inApp: false, kakao: false }, UK)
    expect(screen.getByText('Google Play에서 다운로드')).toHaveAttribute('href', playWebUrl(RK))
  })

  it('other의 버튼 순서는 store, appstore이고 둘 다 btn primary다', () => {
    const { container } = view({ os: 'other', inApp: false, kakao: false })
    const links = [...container.querySelectorAll('.actions a')]
    expect(links.map((a) => a.id)).toEqual(['store', 'appstore'])
    expect(links.map((a) => a.className)).toEqual(['btn primary', 'btn primary'])
  })
})
