import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { LICENSE_TEXT_URL, loadLicenseText, OSS, sortedEntries } from './licenses'
import Oss from './Oss'
import type { Platform } from '../landing/ua'

const view = (os: Platform['os']) => render(<Oss platform={{ os, inApp: false, kakao: false }} />)
const headings = () => screen.getAllByRole('heading', { level: 2 }).map((h) => h.textContent)
const names = (title: string) => {
  const section = screen.getByRole('region', { name: title })
  return Array.from(section.querySelectorAll('summary')).map((s) => s.textContent)
}

describe('Oss', () => {
  it('android는 Font와 Android만 보인다', () => {
    view('android')
    expect(headings()).toEqual(['Font', 'Android'])
  })

  it('ios는 Font와 iOS만 보인다', () => {
    view('ios')
    expect(headings()).toEqual(['Font', 'iOS'])
  })

  it('그 밖에는 셋 다 보인다', () => {
    view('other')
    expect(headings()).toEqual(['Font', 'Android', 'iOS'])
  })

  it('이름은 대소문자를 가리지 않는 문자열 순이다', () => {
    view('other')
    expect(names('Font')).toEqual(['LINE Seed JP', 'LINE Seed TW', 'SUIT'])
    // 소문자로 시작하는 이름(javax, kotlinx)이 맨 뒤로 가지 않는다.
    expect(names('Android').slice(14, 24)).toEqual([
      'Haze',
      'javax.inject',
      'Jetpack Compose',
      'Kakao SDK',
      'Kermit',
      'Kotlin Standard Library',
      'kotlinx-datetime',
      'kotlinx.coroutines',
      'kotlinx.serialization',
      'Lottie for Android',
    ])
    expect(names('iOS').slice(7, 10)).toEqual(['GoogleUtilities', 'gRPC', 'GTMSessionFetcher'])
  })

  it('접혀 있다가 누르면 라이선스, 버전, 주소, 모듈이 보인다', async () => {
    view('ios')
    const summary = screen.getByText('Kakao SDK for iOS')
    const details = summary.closest('details')!
    expect(details.open).toBe(false)
    await userEvent.click(summary)
    expect(details.open).toBe(true)
    const body = within(details)
    expect(body.getByRole('link', { name: /^Apache-2\.0\s*새 탭에서 열려요$/ })).toHaveAttribute('href', LICENSE_TEXT_URL['Apache-2.0'])
    expect(body.getByText('2.28.0')).toBeInTheDocument()
    expect(body.getByRole('link', { name: /^https:\/\/github\.com\/kakao\/kakao-ios-sdk/ })).toHaveAttribute(
      'href',
      'https://github.com/kakao/kakao-ios-sdk',
    )
    expect(body.getByText('KakaoSDKUser')).toBeInTheDocument()
    expect((await body.findByText(/Apache License\s+Version 2\.0/)).tagName).toBe('PRE')
  })

  it('전문은 프로젝트 LICENSE를 그대로 보인다', async () => {
    view('ios')
    await userEvent.click(screen.getByText('Alamofire'))
    expect(await screen.findByText(/^Copyright \(c\) 2014-2022 Alamofire Software Foundation/)).toBeInTheDocument()
  })

  it('전문이 없는 Google 약관은 링크로 안내한다', async () => {
    view('android')
    const summary = screen.getByText('ML Kit Subject Segmentation')
    await userEvent.click(summary)
    const details = within(summary.closest('details')!)
    expect(details.getByText('약관 전문은 위 라이선스 링크에서 볼 수 있어요.')).toBeInTheDocument()
    expect(details.queryByText((_, el) => el?.tagName === 'PRE')).not.toBeInTheDocument()
  })
})

describe('licenses', () => {
  it('전문 키는 모두 texts/에 있고, 종류와 맞는다', async () => {
    const heading: Partial<Record<string, RegExp>> = {
      'Apache-2.0': /Apache License/,
      MIT: /Permission is hereby granted, free of charge/,
      'BSD-3-Clause': /Redistribution and use in source and binary forms/,
      Zlib: /This software is provided 'as-is'/,
      'OFL-1.1': /SIL Open Font License, Version 1\.1/i,
      'MPL-2.0': /Mozilla Public License Version 2\.0/,
    }
    for (const entry of Object.values(OSS).flat()) {
      if (!entry.text) {
        expect(entry.license).toMatch(/Terms|Kit License/)
        continue
      }
      expect(await loadLicenseText(entry.text), entry.name).toMatch(heading[entry.license]!)
    }
  })

  it('NOTICE가 있는 Apache 프로젝트는 전문 뒤에 NOTICE를 싣는다', async () => {
    for (const name of ['Kotlin Standard Library', 'kotlinx.coroutines', 'kotlinx.serialization']) {
      const entry = OSS.android.find((e) => e.name === name)!
      expect(await loadLicenseText(entry.text!), name).toMatch(/NOTICE file corresponding to the section 4 d/)
    }
  })

  it('없는 전문 키는 거부한다', async () => {
    await expect(loadLicenseText('nope')).rejects.toThrow('nope')
  })

  it('섹션 안에서 이름이 겹치지 않는다', () => {
    for (const entries of Object.values(OSS)) {
      const all = entries.map((e) => e.name)
      expect(new Set(all).size).toBe(all.length)
    }
  })

  it('sortedEntries는 원본을 바꾸지 않는다', () => {
    const before = OSS.android.map((e) => e.name)
    sortedEntries(OSS.android)
    expect(OSS.android.map((e) => e.name)).toEqual(before)
  })
})
