import { landingView, type Platform } from './ua'

type Props = { platform: Platform; pageUrl: string }

const asset = (path: string) => `${import.meta.env.BASE_URL}${path}`

export default function Landing({ platform, pageUrl }: Props) {
  const view = landingView(platform, pageUrl)

  return (
    <main>
      <header className="header">
        <div className="platform">
          <span>iOS</span>
          <img src={asset('assets/dot.svg')} alt="" width={8} height={8} />
          <span>Android</span>
        </div>
        <div className="title-block">
          <h1>Parfait</h1>
          <div className="tagline">
            <p>
              우리가 쌓을 하루,
              <br />
              <b>사진 공유 캔버스 SNS</b>
            </p>
            <img
              className="app-icon"
              src={asset('assets/icon-512.png')}
              alt="Parfait 앱 아이콘"
              width={124}
              height={124}
            />
          </div>
        </div>
      </header>

      <section className="actions">
        {view.buttons.map((b) => (
          <a key={b.id} id={b.id} className={b.variant === 'primary' ? 'btn primary' : 'btn'} href={b.href}>
            {b.label}
          </a>
        ))}
        {view.showInAppHint && (
          <p id="inapp-hint" className="hint">
            스토어가 열리지 않으면 오른쪽 위 <b>⋮</b> 메뉴에서 <b>외부 브라우저에서 열기</b>를
            눌러주세요.
          </p>
        )}
      </section>

      <div className="preview">
        <img
          src={asset('assets/hero.jpg')}
          alt="Parfait 캔버스 화면 미리보기"
          width={924}
          height={842}
        />
      </div>
    </main>
  )
}
