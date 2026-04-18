import fs from 'node:fs';
import path from 'node:path';

import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

function readIniSection(content, section) {
  const re = new RegExp(`\\[${section}\\]([\\s\\S]*?)(\\n\\[|$)`, 'i');
  const match = content.match(re);
  return match ? match[1] : '';
}

function getIniValue(sectionContent, key, fallback) {
  const match = sectionContent.match(new RegExp(`^${key}\\s*=\\s*(.+)`, 'im'));
  return match ? match[1].trim() : fallback;
}

function parseConfigIni() {
  const iniPath = path.resolve(__dirname, '..', 'config.ini');
  if (!fs.existsSync(iniPath)) {
    return { apiBaseUrl: 'http://127.0.0.1:8000/api/v1', stripePublishableKey: '' };
  }

  const content = fs.readFileSync(iniPath, 'utf-8');
  const apiSection = readIniSection(content, 'api');
  const stripeSection = readIniSection(content, 'stripe');

  return {
    apiBaseUrl: getIniValue(apiSection, 'base_url', 'http://127.0.0.1:8000/api/v1'),
    stripePublishableKey: getIniValue(stripeSection, 'publishable_key', ''),
  };
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const ini = parseConfigIni();
  const apiBaseUrl = env.VITE_API_BASE_URL || ini.apiBaseUrl;
  const stripeKey = env.VITE_STRIPE_PUBLISHABLE_KEY || ini.stripePublishableKey;

  return {
    plugins: [react()],
    server: {
      port: 5173
    },
    define: {
      'import.meta.env.VITE_API_BASE_URL': JSON.stringify(apiBaseUrl),
      'import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY': JSON.stringify(stripeKey),
    }
  };
});
