import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      // Spring 백엔드는 8080. 쿠키(refresh) 전달을 위해 same-origin 프록시 사용.
      '/api': 'http://localhost:8080',
    },
  },
})
