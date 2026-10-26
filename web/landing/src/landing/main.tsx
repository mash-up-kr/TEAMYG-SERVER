import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { startAnalytics } from './analytics'
import Landing from './Landing'
import './landing.css'
import { detectPlatform } from './ua'

startAnalytics()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Landing
      platform={detectPlatform(navigator.userAgent, navigator.maxTouchPoints)}
      pageUrl={location.href}
    />
  </StrictMode>,
)
