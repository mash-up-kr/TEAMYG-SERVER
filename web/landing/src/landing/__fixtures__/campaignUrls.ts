export const BASE = 'https://parfait-app.store/'
const A100 = 'a'.repeat(100)
const ALL = 'utm_source=instagram&utm_medium=social&utm_campaign=202610-launch&utm_content=story&utm_term=kw&utm_id=abc.1'

export const CAMPAIGN_CASES: readonly (readonly [name: string, pageUrl: string, referrer: string])[] = [
  ['no query', BASE, ''],
  ['all six keys', `${BASE}?${ALL}`, ALL],
  ['keys out of order with a foreign key', `${BASE}?utm_campaign=c&x=1&utm_source=s`, 'utm_source=s&utm_campaign=c'],
  ['no source', `${BASE}?utm_medium=social&utm_campaign=c`, ''],
  ['a disallowed character', `${BASE}?utm_source=s&utm_campaign=a;b`, 'utm_source=s'],
  ['a 100-character value', `${BASE}?utm_source=s&utm_campaign=${A100}`, `utm_source=s&utm_campaign=${A100}`],
  ['a 101-character value', `${BASE}?utm_source=s&utm_campaign=${A100}a`, 'utm_source=s'],
  ['a repeated key whose first piece is bad', `${BASE}?utm_source=s&utm_campaign=a%2Db&utm_campaign=ok`, 'utm_source=s'],
  ['a repeated source whose first piece is bad', `${BASE}?utm_source=a%20b&utm_source=good`, ''],
  ['a repeated key whose first piece is good', `${BASE}?utm_source=first&utm_source=second`, 'utm_source=first'],
  ['a fragment after the query', `${BASE}?utm_source=s#utm_campaign=c`, 'utm_source=s'],
  ['a query inside the fragment', `${BASE}#?utm_source=s`, ''],
  ['a percent-encoded value', `${BASE}?utm_source=s&utm_campaign=a%2Db`, 'utm_source=s'],
  ['a plus sign', `${BASE}?utm_source=a+b&utm_campaign=c`, ''],
  ['a percent-encoded key', `${BASE}?utm%5Fsource=x&utm_campaign=c`, ''],
  ['an upper-case key', `${BASE}?UTM_SOURCE=x&utm_campaign=c`, ''],
  ['an empty source', `${BASE}?utm_source=&utm_campaign=c`, ''],
  ['an empty optional value', `${BASE}?utm_source=s&utm_campaign=`, 'utm_source=s'],
  ['a key without =', `${BASE}?utm_source&utm_campaign=c`, ''],
  ['a key without = before a good piece', `${BASE}?utm_source&utm_source=s`, 'utm_source=s'],
  ['a broken percent sequence', `${BASE}?utm_source=s&utm_campaign=%E0%`, 'utm_source=s'],
  ['no question mark', 'https://x/y&utm_source=s', ''],
  ['a second = in the value', `${BASE}?utm_source=a=b`, ''],
  ['a second ? in the value', `${BASE}?utm_source=s?utm_campaign=c`, ''],
  ['every allowed punctuation mark', `${BASE}?utm_source=A.b_c~d-e`, 'utm_source=A.b_c~d-e'],
  ['empty pieces', `${BASE}?&&utm_source=s&`, 'utm_source=s'],
  ['object-prototype keys', `${BASE}?constructor=x&__proto__=y&utm_source=s`, 'utm_source=s'],
]
