import fs from 'node:fs';
import path from 'node:path';

import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

function parseApiBaseUrlFromIni() {
  const iniPath = path.resolve(__dirname, '..', 'config.ini');
  if (!fs.existsSync(iniPath)) {
    return 'http://127.0.0.1:8000/api/v1';
  }

  const content = fs.readFileSync(iniPath, 'utf-8');
  const apiSection = content.match(/\[api\]([\s\S]*?)(\n\[|$)/i);
  if (!apiSection) {
    return 'http://127.0.0.1:8000/api/v1';
  }

  const baseUrlMatch = apiSection[1].match(/base_url\s*=\s*(.+)/i);
  return baseUrlMatch ? baseUrlMatch[1].trim() : 'http://127.0.0.1:8000/api/v1';
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const apiBaseUrl = env.VITE_API_BASE_URL || parseApiBaseUrlFromIni();

  return {
    plugins: [react()],
    server: {
      port: 5173
    },
    define: {
      'import.meta.env.VITE_API_BASE_URL': JSON.stringify(apiBaseUrl)
    }
  };
});
