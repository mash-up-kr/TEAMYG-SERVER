import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import Landing from './Landing'
import './landing.css'
import { detectPlatform } from './ua'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Landing
      platform={detectPlatform(navigator.userAgent, navigator.maxTouchPoints)}
      pageUrl={location.href}
    />
  </StrictMode>,
)
