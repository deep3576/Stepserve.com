import { useMemo, useState } from 'react';
import axios from 'axios';
import HeroCard from './components/HeroCard';
import NavTabs from './components/NavTabs';
import SearchPanel from './components/panels/SearchPanel';
import CustomerPanel from './components/panels/CustomerPanel';
import HandymanPanel from './components/panels/HandymanPanel';
import AdminPanel from './components/panels/AdminPanel';

export default function App() {
  const [tab, setTab] = useState('search');
  const [baseUrl, setBaseUrl] = useState('http://127.0.0.1:8000/api/v1');
  const [token, setToken] = useState('');

  const api = useMemo(() => {
    const client = axios.create({ baseURL: baseUrl });
    client.interceptors.request.use((config) => {
      if (token) config.headers.Authorization = `Bearer ${token}`;
      return config;
    });
    return client;
  }, [baseUrl, token]);

  return (
    <div className="app-shell">
      <div className="glow glow-a" />
      <div className="glow glow-b" />

      <main>
        <HeroCard
          title="Stepserve Operations Console"
          subtitle="Search, customer journey, handyman uploads, and admin controls from one modern workspace."
        />

        <section className="panel settings">
          <h3>Connection</h3>
          <div className="grid-form">
            <input value={baseUrl} onChange={(e) => setBaseUrl(e.target.value)} placeholder="API base URL" />
            <input value={token} onChange={(e) => setToken(e.target.value)} placeholder="JWT access token" />
          </div>
        </section>

        <NavTabs active={tab} onChange={setTab} />

        {tab === 'search' ? <SearchPanel api={api} /> : null}
        {tab === 'customer' ? <CustomerPanel api={api} /> : null}
        {tab === 'handyman' ? <HandymanPanel api={api} /> : null}
        {tab === 'admin' ? <AdminPanel api={api} /> : null}
      </main>
    </div>
  );
}
