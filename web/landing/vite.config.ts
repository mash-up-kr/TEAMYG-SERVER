import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// parfait-app.store 루트에서 서빙되므로 base는 '/'.
export default defineConfig({
  base: '/',
  plugins: [react()],
  build: {
    outDir: 'dist',
    // /는 랜딩, /oss는 오픈소스 라이선스 페이지. 키 index는 랜딩 번들 이름(index-*.js)을 그대로 두려고.
    rollupOptions: {
      input: { index: 'index.html', oss: 'oss/index.html' },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['src/test/setup.ts'],
  },
})
