import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    port: 3000,
    hmr: {
      host: process.env.VITE_HMR_HOST || 'localhost',
      clientPort: parseInt(process.env.VITE_HMR_CLIENT_PORT || '3001'),
      path: `/hmr/${process.env.PROJECT_ID || 'dev'}`,
    },
    proxy: {
      '/api': {
        target: 'http://localhost:4000',
        changeOrigin: true,
      }
    }
  }
})
