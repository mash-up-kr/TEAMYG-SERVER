import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// parfait-app.store 루트에서 서빙되므로 base는 '/'.
export default defineConfig({
  base: '/',
  plugins: [react()],
  build: {
    outDir: 'dist',
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['src/test/setup.ts'],
  },
})
