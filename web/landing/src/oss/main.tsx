import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { detectPlatform } from '../landing/ua'
import Oss from './Oss'
import './oss.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Oss platform={detectPlatform(navigator.userAgent, navigator.maxTouchPoints)} />
  </StrictMode>,
)
