import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import Landing from './Landing'
import { playIntentUrl, type Platform } from './ua'

const view = (platform: Platform) => render(<Landing platform={platform} pageUrl="https://x/y" />)

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
})
