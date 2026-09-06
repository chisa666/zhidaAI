import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  cacheDir: 'D:/Environment/zhida-vite-cache',
  resolve: { alias: { '@': '/src' } },
  server: { port: 5173, proxy: { '/api': 'http://localhost:8080' } }
})
