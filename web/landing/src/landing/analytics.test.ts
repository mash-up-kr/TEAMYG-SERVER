import { afterEach, describe, expect, it } from 'vitest'
import { analyticsLocation, GA_MEASUREMENT_ID, startAnalytics } from './analytics'

type GtagWindow = Window & { dataLayer?: unknown[]; gtag?: (...args: unknown[]) => void }

const gtagScripts = () => document.head.querySelectorAll<HTMLScriptElement>('script[src*="googletagmanager"]')
const queued = () => (window as GtagWindow).dataLayer!.map((entry) => Array.from(entry as ArrayLike<unknown>))

afterEach(() => {
  gtagScripts().forEach((s) => s.remove())
  delete (window as GtagWindow).dataLayer
  delete (window as GtagWindow).gtag
  window.history.replaceState(null, '', '/')
})

it('loads gtag.js for the landing measurement id', () => {
  startAnalytics()
  const scripts = gtagScripts()
  expect(scripts).toHaveLength(1)
  expect(scripts[0].src).toBe(`https://www.googletagmanager.com/gtag/js?id=${GA_MEASUREMENT_ID}`)
  expect(scripts[0].async).toBe(true)
})

it('queues js and config as arguments objects, which gtag.js requires', () => {
  startAnalytics()
  const raw = (window as GtagWindow).dataLayer!
  expect(raw).toHaveLength(2)
  for (const entry of raw) {
    expect(Array.isArray(entry)).toBe(false)
    expect(Object.prototype.toString.call(entry)).toBe('[object Arguments]')
  }
  expect(queued()[0][0]).toBe('js')
  expect(queued()[1].slice(0, 2)).toEqual(['config', GA_MEASUREMENT_ID])
})

it('sends only the UTM part of the address as page_location', () => {
  window.history.replaceState(null, '', '/?utm_source=instagram&gclid=abc&utm_medium=social#top')
  startAnalytics()
  expect(queued()[1][2]).toEqual({
    page_location: `${window.location.origin}/?utm_source=instagram&utm_medium=social`,
  })
})

it('keeps what an earlier snippet already queued and still configures after it', () => {
  ;(window as GtagWindow).dataLayer = ['earlier']
  startAnalytics()
  const raw = (window as GtagWindow).dataLayer!
  expect(raw).toHaveLength(3)
  expect(raw[0]).toBe('earlier')
  expect(queued()[2].slice(0, 2)).toEqual(['config', GA_MEASUREMENT_ID])
  expect(gtagScripts()).toHaveLength(1)
})

it('does nothing the second time, so page_view is not sent twice', () => {
  startAnalytics()
  startAnalytics()
  expect(gtagScripts()).toHaveLength(1)
  expect((window as GtagWindow).dataLayer).toHaveLength(2)
})

describe('analyticsLocation', () => {
  const PAGE = 'https://parfait-app.store/'
  it.each([
    ['no query', PAGE, PAGE],
    ['only UTM keys', `${PAGE}?utm_source=ig&utm_campaign=c`, `${PAGE}?utm_source=ig&utm_campaign=c`],
    ['foreign keys dropped', `${PAGE}?fbclid=x&utm_source=ig&email=a%40b.c`, `${PAGE}?utm_source=ig`],
    ['nothing left after dropping', `${PAGE}?gclid=x`, PAGE],
    ['a fragment', `${PAGE}?utm_source=ig#utm_medium=m`, `${PAGE}?utm_source=ig`],
    ['a key without =', `${PAGE}?utm_sourcex&utm_source=ig`, `${PAGE}?utm_source=ig`],
    ['an encoded value kept as is', `${PAGE}?utm_campaign=%EC%8B%A0`, `${PAGE}?utm_campaign=%EC%8B%A0`],
    ['an upper-case key dropped', `${PAGE}?UTM_SOURCE=ig`, PAGE],
  ])('%s', (_name, pageUrl, expected) => {
    expect(analyticsLocation(pageUrl)).toBe(expected)
  })
})
