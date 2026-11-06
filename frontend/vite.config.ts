import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  // Backend CORS only allows http://localhost:5173, so the port must not change.
  server: { port: 5173, strictPort: true },
})