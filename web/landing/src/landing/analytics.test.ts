import { afterEach, expect, it } from 'vitest'
import { GA_MEASUREMENT_ID, startAnalytics } from './analytics'

type GtagWindow = Window & { dataLayer?: unknown[]; gtag?: (...args: unknown[]) => void }

afterEach(() => {
  document.head.querySelectorAll('script[src*="googletagmanager"]').forEach((s) => s.remove())
  delete (window as GtagWindow).dataLayer
  delete (window as GtagWindow).gtag
})

it('loads gtag.js for the landing measurement id', () => {
  startAnalytics()
  const script = document.head.querySelector<HTMLScriptElement>('script[src*="googletagmanager"]')
  expect(script?.src).toBe(`https://www.googletagmanager.com/gtag/js?id=${GA_MEASUREMENT_ID}`)
  expect(script?.async).toBe(true)
})

it('queues js and config as arguments objects, which gtag.js requires', () => {
  startAnalytics()
  const queued = (window as GtagWindow).dataLayer!
  expect(queued).toHaveLength(2)
  for (const entry of queued) {
    expect(Array.isArray(entry)).toBe(false)
    expect(Object.prototype.toString.call(entry)).toBe('[object Arguments]')
  }
  expect(Array.from(queued[0] as ArrayLike<unknown>)[0]).toBe('js')
  expect(Array.from(queued[1] as ArrayLike<unknown>)).toEqual(['config', GA_MEASUREMENT_ID])
})

it('keeps what an earlier snippet already queued', () => {
  ;(window as GtagWindow).dataLayer = ['earlier']
  startAnalytics()
  expect((window as GtagWindow).dataLayer![0]).toBe('earlier')
})
