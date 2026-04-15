import { useEffect, useMemo, useState } from 'react';
import axios from 'axios';

const DEFAULT_API_BASE_URL = typeof __STEP_API_BASE_URL__ === 'string' ? __STEP_API_BASE_URL__ : 'http://127.0.0.1:8000/api/v1';

function formatPrice(price) {
  const n = Number(price || 0);
  return new Intl.NumberFormat('en-CA', { style: 'currency', currency: 'CAD', maximumFractionDigits: 0 }).format(n);
}

function ListingCard({ item }) {
  return (
    <article className="listing-card">
      <div className="thumb">{item.title?.slice(0, 2).toUpperCase() || 'AD'}</div>
      <div className="listing-body">
        <h4>{item.title}</h4>
        <p>{item.description}</p>
        <div className="meta-row">
          <strong>{formatPrice(item.price)}</strong>
          <span>{item.location || 'Nearby'}</span>
        </div>
        <small>{item.provider_name || 'Stepserve seller'}</small>
      </div>
    </article>
  );
}

export default function App() {
  const [baseUrl, setBaseUrl] = useState(DEFAULT_API_BASE_URL);
  const [token, setToken] = useState('');
  const [currentUser, setCurrentUser] = useState(null);
  const [auth, setAuth] = useState({ mode: 'login', email: '', password: '', role: 'customer' });

  const [search, setSearch] = useState({ query: '', location: '', min_price: '', max_price: '' });
  const [home, setHome] = useState({ categories: [], featured: [], latest: [], top_locations: [] });
  const [results, setResults] = useState([]);

  const [providerProfile, setProviderProfile] = useState({ full_name: '', bio: '', location: '', hourly_rate: '' });
  const [adForm, setAdForm] = useState({ category_id: '', title: '', description: '', price: '' });

  const [adminOverview, setAdminOverview] = useState(null);
  const [adminUsers, setAdminUsers] = useState([]);
  const [adminBookings, setAdminBookings] = useState([]);

  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');

  const api = useMemo(() => {
    const client = axios.create({ baseURL: baseUrl });
    client.interceptors.request.use((config) => {
      if (token) config.headers.Authorization = `Bearer ${token}`;
      return config;
    });
    return client;
  }, [baseUrl, token]);

  const loadHome = async () => {
    try {
      const { data } = await api.get('/stepserve/home');
      setHome(data);
      setResults(data.latest || []);
      setError('');
    } catch {
      setError('Could not load Stepserve feed. Check API base URL and backend status.');
    }
  };

  const loadMe = async () => {
    if (!token) {
      setCurrentUser(null);
      return;
    }
    try {
      const { data } = await api.get('/auth/me');
      setCurrentUser(data);
    } catch {
      setCurrentUser(null);
      setError('Session is invalid. Please login again.');
    }
  };

  useEffect(() => {
    loadHome();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [api]);

  useEffect(() => {
    loadMe();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  const runSearch = async (e) => {
    e.preventDefault();
    try {
      const { data } = await api.get('/search/services', {
        params: {
          query: search.query || undefined,
          location: search.location || undefined,
          min_price: search.min_price || undefined,
          max_price: search.max_price || undefined
        }
      });
      setResults(data);
      setError('');
    } catch {
      setError('Search failed. Check your filters and API connectivity.');
    }
  };

  const quickLocationSearch = async (location) => {
    setSearch((prev) => ({ ...prev, location }));
    try {
      const { data } = await api.get('/search/services', { params: { location } });
      setResults(data);
    } catch {
      setError('Unable to filter by location right now.');
    }
  };

  const submitAuth = async (e) => {
    e.preventDefault();
    setNotice('');
    setError('');
    try {
      const endpoint = auth.mode === 'register' ? '/auth/register' : '/auth/login';
      const payload = auth.mode === 'register'
        ? { email: auth.email, password: auth.password, role: auth.role }
        : { email: auth.email, password: auth.password };
      const { data } = await api.post(endpoint, payload);
      setToken(data.access_token);
      setNotice(auth.mode === 'register' ? 'Account created and logged in.' : 'Logged in successfully.');
    } catch (err) {
      setError(err?.response?.data?.detail || 'Authentication failed.');
    }
  };

  const logout = () => {
    setToken('');
    setCurrentUser(null);
    setNotice('Logged out. Public browsing is still available.');
  };

  const upsertProviderProfile = async (e) => {
    e.preventDefault();
    setNotice('');
    setError('');
    try {
      await api.post('/providers/profile', {
        ...providerProfile,
        hourly_rate: providerProfile.hourly_rate ? Number(providerProfile.hourly_rate) : null
      });
      setNotice('Handyman profile saved. You can post ads now.');
    } catch (err) {
      setError(err?.response?.data?.detail || 'Could not save provider profile.');
    }
  };

  const createAd = async (e) => {
    e.preventDefault();
    setNotice('');
    setError('');
    try {
      await api.post('/services', {
        category_id: Number(adForm.category_id),
        title: adForm.title,
        description: adForm.description,
        price: Number(adForm.price)
      });
      setNotice('Ad posted successfully on Stepserve.');
      setAdForm({ category_id: '', title: '', description: '', price: '' });
      await loadHome();
    } catch (err) {
      setError(err?.response?.data?.detail || 'Could not post ad. Ensure provider account/profile exists.');
    }
  };

  const loadAdminPanel = async () => {
    try {
      const [overviewRes, usersRes, bookingsRes] = await Promise.all([
        api.get('/admin/overview'),
        api.get('/admin/users'),
        api.get('/admin/bookings')
      ]);
      setAdminOverview(overviewRes.data);
      setAdminUsers(usersRes.data || []);
      setAdminBookings(bookingsRes.data || []);
      setError('');
    } catch (err) {
      setError(err?.response?.data?.detail || 'Unable to load admin panel data.');
    }
  };

  useEffect(() => {
    if (currentUser?.role === 'admin') {
      loadAdminPanel();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentUser, api]);

  return (
    <div className="karrot-shell">
      <header className="topbar">
        <div className="brand">Stepserve</div>
        <nav>
          <a href="#featured">Featured</a>
          <a href="#latest">Latest</a>
          <a href="#account">Account</a>
        </nav>
        <button type="button" className="sell-cta" onClick={() => document.getElementById('account')?.scrollIntoView({ behavior: 'smooth' })}>Sell on Stepserve</button>
      </header>

      <section className="hero">
        <div>
          <h1>Your local neighborhood marketplace, powered by Stepserve</h1>
          <p>Karrot-inspired local-first design with search-first browsing, secure role-based posting, and admin controls.</p>
          <form className="search-row" onSubmit={runSearch}>
            <input placeholder="Search ads" value={search.query} onChange={(e) => setSearch({ ...search, query: e.target.value })} />
            <input placeholder="Neighborhood" value={search.location} onChange={(e) => setSearch({ ...search, location: e.target.value })} />
            <input placeholder="Min $" value={search.min_price} onChange={(e) => setSearch({ ...search, min_price: e.target.value })} />
            <input placeholder="Max $" value={search.max_price} onChange={(e) => setSearch({ ...search, max_price: e.target.value })} />
            <button type="submit">Search</button>
          </form>
        </div>
        <div className="hero-card">
          <h3>Stepserve nearby stats</h3>
          <p><strong>{home.latest.length}</strong> active local ads</p>
          <p><strong>{home.categories.length}</strong> categories</p>
          <p><strong>{home.top_locations.length}</strong> popular locations</p>
          <div className="connection-box">
            <input value={baseUrl} onChange={(e) => setBaseUrl(e.target.value)} placeholder="API base URL" />
            <input value={token} onChange={(e) => setToken(e.target.value)} placeholder="JWT token" />
          </div>
        </div>
      </section>

      <section className="category-strip">
        {home.categories.map((cat) => (
          <button key={cat.id} type="button" onClick={() => setSearch((prev) => ({ ...prev, query: cat.name }))}>
            {cat.name}<span>{cat.services_count}</span>
          </button>
        ))}
      </section>

      {notice ? <p className="success-banner">{notice}</p> : null}
      {error ? <p className="error-banner">{error}</p> : null}

      <main className="content-grid">
        <aside className="side-panel" id="account">
          <h3>{currentUser ? `Signed in as ${currentUser.role}` : 'Login / Register'}</h3>
          <form className="auth-form" onSubmit={submitAuth}>
            <div className="mode-toggle">
              <button type="button" className={auth.mode === 'login' ? 'active' : ''} onClick={() => setAuth((s) => ({ ...s, mode: 'login' }))}>Login</button>
              <button type="button" className={auth.mode === 'register' ? 'active' : ''} onClick={() => setAuth((s) => ({ ...s, mode: 'register' }))}>Register</button>
            </div>
            <input placeholder="Email" type="email" value={auth.email} onChange={(e) => setAuth((s) => ({ ...s, email: e.target.value }))} required />
            <input placeholder="Password" type="password" value={auth.password} onChange={(e) => setAuth((s) => ({ ...s, password: e.target.value }))} required />
            {auth.mode === 'register' ? (
              <select value={auth.role} onChange={(e) => setAuth((s) => ({ ...s, role: e.target.value }))}>
                <option value="customer">Customer</option>
                <option value="provider">Handyman/Provider</option>
              </select>
            ) : null}
            <button type="submit">{auth.mode === 'register' ? 'Create account' : 'Login'}</button>
          </form>
          {currentUser ? <button className="ghost" type="button" onClick={logout}>Logout</button> : null}

          <h3>Nearby areas</h3>
          <ul>
            {(home.top_locations || []).map((loc) => (
              <li key={loc.location}>
                <button type="button" onClick={() => quickLocationSearch(loc.location)}>{loc.location}</button>
                <span>{loc.listings_count}</span>
              </li>
            ))}
          </ul>
        </aside>

        <section>
          {currentUser?.role === 'provider' ? (
            <div className="panel-grid">
              <section className="role-panel">
                <h3>Handyman profile (required for posting)</h3>
                <form className="stack-form" onSubmit={upsertProviderProfile}>
                  <input placeholder="Full name" value={providerProfile.full_name} onChange={(e) => setProviderProfile({ ...providerProfile, full_name: e.target.value })} required />
                  <input placeholder="Location" value={providerProfile.location} onChange={(e) => setProviderProfile({ ...providerProfile, location: e.target.value })} />
                  <input placeholder="Hourly rate" value={providerProfile.hourly_rate} onChange={(e) => setProviderProfile({ ...providerProfile, hourly_rate: e.target.value })} />
                  <textarea placeholder="Bio" value={providerProfile.bio} onChange={(e) => setProviderProfile({ ...providerProfile, bio: e.target.value })} />
                  <button type="submit">Save profile</button>
                </form>
              </section>

              <section className="role-panel">
                <h3>Post new ad</h3>
                <form className="stack-form" onSubmit={createAd}>
                  <select value={adForm.category_id} onChange={(e) => setAdForm({ ...adForm, category_id: e.target.value })} required>
                    <option value="">Select category</option>
                    {home.categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                  </select>
                  <input placeholder="Title" value={adForm.title} onChange={(e) => setAdForm({ ...adForm, title: e.target.value })} required />
                  <textarea placeholder="Description" value={adForm.description} onChange={(e) => setAdForm({ ...adForm, description: e.target.value })} required />
                  <input placeholder="Price" value={adForm.price} onChange={(e) => setAdForm({ ...adForm, price: e.target.value })} required />
                  <button type="submit">Publish ad</button>
                </form>
              </section>
            </div>
          ) : null}

          {currentUser?.role === 'admin' ? (
            <section className="role-panel">
              <h3>Stepserve Admin Panel</h3>
              <div className="admin-stats">
                <span>Users {adminOverview?.users_count ?? '-'}</span>
                <span>Services {adminOverview?.services_count ?? '-'}</span>
                <span>Bookings {adminOverview?.bookings_count ?? '-'}</span>
                <span>Paid {formatPrice(adminOverview?.paid_total ?? 0)}</span>
              </div>
              <div className="admin-grid">
                <ul className="compact-list">
                  {adminUsers.slice(0, 10).map((u) => <li key={u.id}>{u.email} · {u.role}</li>)}
                </ul>
                <ul className="compact-list">
                  {adminBookings.slice(0, 10).map((b) => <li key={b.id}>#{b.id} · {b.status} · {formatPrice(b.total_price)}</li>)}
                </ul>
              </div>
            </section>
          ) : null}

          <div className="section-head" id="featured">
            <h3>Featured in your neighborhood</h3>
            <small>{home.featured.length} listings</small>
          </div>
          <div className="cards-grid cards-grid-featured">
            {(home.featured || []).map((item) => <ListingCard key={`f-${item.id}`} item={item} />)}
          </div>

          <div className="section-head" id="latest">
            <h3>Latest local ads</h3>
            <small>{results.length} listings</small>
          </div>
          <div className="cards-grid">
            {results.map((item) => <ListingCard key={`r-${item.id}`} item={item} />)}
          </div>
        </section>
      </main>
    </div>
  );
}
