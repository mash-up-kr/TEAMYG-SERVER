import { expect, it, vi } from 'vitest'
import html from '../../index.html?raw'
import { UA_PRESETS } from './uaPresets'
import { BASE, CAMPAIGN_CASES } from './__fixtures__/campaignUrls'
import { detectPlatform, landingView, playIntentUrl } from './ua'

const ANDROID = UA_PRESETS.find((p) => p.id === 'android-chrome')!.ua
const MIXED: [string, string][] = [
  ['android and iphone tokens', `${ANDROID} like iPhone`],
  ['android and kakaotalk', `${ANDROID} KAKAOTALK/10.0.0`],
  ['android and line', `${ANDROID} Line/13.0.0`],
  ['lower-case android', 'mozilla/5.0 (linux; android 14) chrome/124.0 mobile'],
  ['lower-case iphone with android', 'mozilla/5.0 (linux; android 14) like iphone'],
]
const UAS: [string, string][] = [...UA_PRESETS.map((p): [string, string] => [p.id, p.ua]), ...MIXED]

function inlineBody(): string {
  const found = [...html.matchAll(/<script(?![^>]*\ssrc=)[^>]*>([\s\S]*?)<\/script>/g)]
  expect(found).toHaveLength(1)
  return found[0][1]
}

function run(nav: object, loc: { replace: ReturnType<typeof vi.fn>; href?: string }): string | null {
  new Function('navigator', 'location', inlineBody())(nav, loc)
  expect(loc.replace.mock.calls.length).toBeLessThanOrEqual(1)
  return (loc.replace.mock.calls[0]?.[0] as string | undefined) ?? null
}

const at = (ua: string, href: string) => run({ userAgent: ua }, { href, replace: vi.fn() })

it.each(UAS)('redirects like landingView for %s', (_id, ua) => {
  for (const [name, pageUrl] of CAMPAIGN_CASES) {
    expect(at(ua, pageUrl), name).toBe(landingView(detectPlatform(ua, 0), pageUrl).autoRedirect)
  }
})

it('really carries the referrer (the comparison above is not vacuous)', () => {
  const [, pageUrl, referrer] = CAMPAIGN_CASES.find(([name]) => name === 'all six keys')!
  expect(at(ANDROID, pageUrl)).toBe(playIntentUrl(referrer))
  expect(at(ANDROID, pageUrl)).toContain('referrer=utm_source%3Dinstagram')
})

it('falls back to the plain intent when reading the address throws', () => {
  const loc = {
    replace: vi.fn(),
    get href(): string {
      throw new Error('blocked')
    },
  }
  expect(run({ userAgent: ANDROID }, loc)).toBe(playIntentUrl())
})

it('does nothing and does not throw without a user agent', () => {
  expect(run({}, { href: BASE, replace: vi.fn() })).toBeNull()
})

// What ES5 does not have, and the newer APIs the script must not lean on.
const NOT_ES5: [string, RegExp, string][] = [
  ['an arrow function', /=>/, 'var f = (a) => a;'],
  ['const', /\bconst\b/, 'const a = 1;'],
  ['let', /\blet\b/, 'let a = 1;'],
  ['a template literal', /`/, 'var a = `x`;'],
  ['URLSearchParams', /URLSearchParams/, 'new URLSearchParams(q);'],
  ['URL', /new URL\b/, 'new URL(href);'],
  ['includes', /\.includes\(/, 'keys.includes(key);'],
  ['startsWith', /\.startsWith\(/, 'ua.startsWith("x");'],
  ['endsWith', /\.endsWith\(/, 'ua.endsWith("x");'],
  ['for of', /\bfor\s*\([^)]*\bof\b/, 'for (var piece of pieces) {}'],
  ['spread or rest', /\.\.\./, 'f(...args);'],
  ['a class', /\bclass\s/, 'class A {}'],
  [
    'an object-literal shorthand method',
    /[{,]\s*(?!(?:if|for|while|switch|catch|with|function)\b)[A-Za-z_$][\w$]*\s*\([^)]*\)\s*\{/,
    'var o = { a: 1, run(x) { return x; } };',
  ],
]

it.each(NOT_ES5)('is written in ES5: no %s', (_name, pattern, sample) => {
  // Comments are not checked.
  const code = inlineBody()
    .replace(/\/\*[\s\S]*?\*\//g, '')
    .replace(/^\s*\/\/.*$/gm, '')
  expect(code).not.toMatch(pattern)
  // The pattern does catch what it names.
  expect(sample).toMatch(pattern)
})

it('does not take ES5 control flow for a shorthand method', () => {
  const shorthand = NOT_ES5[NOT_ES5.length - 1][1]
  expect('if (a) { if (b) { c(); } }').not.toMatch(shorthand)
  expect('try { f(); } catch (e) { g(); }').not.toMatch(shorthand)
  expect('var o = { run: function (x) { return x; } };').not.toMatch(shorthand)
})

// Accepted divergence (spec section 3): a user agent with both Macintosh and Android on a touch
// device is iOS for detectPlatform(ua, touchPoints), but the script has no touch points and redirects.
it('redirects a touch device whose user agent has both Macintosh and Android, unlike detectPlatform', () => {
  const ua = 'Mozilla/5.0 (Macintosh; Linux; Android 14) Chrome/124.0 Mobile'
  expect(detectPlatform(ua, 5).os).toBe('ios')
  expect(at(ua, BASE)).toBe(playIntentUrl())
})
