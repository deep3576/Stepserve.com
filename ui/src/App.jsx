import { useState, useEffect, useMemo } from 'react'
import Nav from './components/Nav'
import Footer from './components/Footer'
import {
  getStoredToken, storeToken, clearToken, createApiClient,
  serviceToProvider, CATEGORY_META,
  apiRegister, apiLogin, apiGetMe, apiSaveProviderProfile, apiGetProviderDashboard,
  apiGetCategories, apiGetHome, apiSearchServices, apiCreateService,
  apiPayListing, apiUpdateListing, apiDeactivateListing, apiGetProviderListings,
  apiAdminOverview, apiAdminUsers, apiAdminBookings, apiAdminUpdateUserStatus,
  apiAdminCreateCategory,
} from './api'

// ── Design tokens ──────────────────────────────────────────
const G = '#0a7c5c', GD = '#085e47', GL = '#f2f9f6'
const st = {
  btnG: { background: G, color: '#fff', border: 'none', borderRadius: 8, padding: '10px 20px', cursor: 'pointer', fontSize: 14, fontWeight: 500 },
  btnO: { background: '#fff', color: '#333', border: '1.5px solid #ddd', borderRadius: 8, padding: '9px 18px', cursor: 'pointer', fontSize: 13, fontWeight: 500 },
  btnSm: { background: G, color: '#fff', border: 'none', borderRadius: 7, padding: '7px 14px', cursor: 'pointer', fontSize: 13, fontWeight: 500 },
  input: { border: '1.5px solid #ddd', borderRadius: 8, padding: '10px 14px', fontSize: 14, width: '100%', outline: 'none', fontFamily: 'inherit', background: '#fff' },
  label: { fontSize: 13, fontWeight: 600, color: '#555', display: 'block', marginBottom: 5 },
  card: { border: '1px solid #eee', borderRadius: 12, padding: '20px', background: '#fff' },
  wrap: { maxWidth: 1140, margin: '0 auto', padding: '32px 20px' },
  h1: { fontSize: 28, fontWeight: 700, letterSpacing: -0.5, marginBottom: 12, color: '#1a1a1a' },
  h2: { fontSize: 21, fontWeight: 700, letterSpacing: -0.3, marginBottom: 14, color: '#1a1a1a' },
  h3: { fontSize: 16, fontWeight: 600, marginBottom: 8, color: '#1a1a1a' },
  muted: { color: '#888', fontSize: 14 },
  badge: (color = '#e1f5ee', text = G) => ({ background: color, color: text, fontSize: 11, fontWeight: 600, padding: '3px 9px', borderRadius: 20, display: 'inline-block' }),
  grid2: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 },
}

// ── Helpers ─────────────────────────────────────────────────
const Stars = ({ n = 5 }) => <span style={{ color: '#f5a623', fontSize: 13 }}>{Array(n).fill('★').join('')}{Array(5 - n).fill('☆').join('')}</span>

const Spinner = () => (
  <div style={{ display: 'flex', justifyContent: 'center', padding: '48px 0' }}>
    <div style={{ width: 32, height: 32, border: `3px solid #eee`, borderTop: `3px solid ${G}`, borderRadius: '50%', animation: 'spin 0.7s linear infinite' }} />
    <style>{`@keyframes spin { to { transform: rotate(360deg) } }`}</style>
  </div>
)

const Banner = ({ msg, type = 'success' }) => msg ? (
  <div style={{ margin: '0 0 16px', padding: '11px 16px', borderRadius: 8, fontSize: 14, background: type === 'success' ? '#ecfff2' : '#fff0f0', border: `1px solid ${type === 'success' ? '#bdeccd' : '#ffd6db'}`, color: type === 'success' ? '#065f46' : '#9f1239' }}>
    {msg}
  </div>
) : null

// ── ProviderCard ────────────────────────────────────────────
const ProviderCard = ({ p, go }) => (
  <div onClick={() => go('/providers/' + p.id)}
    style={{ border: '1px solid #eee', borderRadius: 12, overflow: 'hidden', cursor: 'pointer', background: '#fff', transition: 'box-shadow 0.15s' }}
    onMouseEnter={e => e.currentTarget.style.boxShadow = '0 4px 16px rgba(0,0,0,0.09)'}
    onMouseLeave={e => e.currentTarget.style.boxShadow = 'none'}>
    <div style={{ background: p.bg, aspectRatio: '1', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 44, position: 'relative' }}>
      {p.icon}
      {p.cert && <span style={{ ...st.badge(), position: 'absolute', top: 8, left: 8 }}>✓ Certified</span>}
      {p.insured && !p.cert && <span style={{ ...st.badge('#dbeafe', '#1a6eb5'), position: 'absolute', top: 8, left: 8 }}>✓ Insured</span>}
    </div>
    <div style={{ padding: '11px 13px 14px' }}>
      <div style={{ fontSize: 14, fontWeight: 600, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{p.name}</div>
      <div style={{ fontSize: 12, color: '#888', margin: '3px 0' }}>{p.city} · {p.cat}</div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 5, marginTop: 5 }}><Stars n={p.rating} /><span style={{ fontSize: 12, color: '#aaa' }}>({p.reviews})</span></div>
      <div style={{ fontSize: 13, fontWeight: 600, color: G, marginTop: 5 }}>{p.price}</div>
    </div>
  </div>
)

// ── CatTabs ─────────────────────────────────────────────────
const CatTabs = ({ active, setActive, categories }) => {
  const tabs = ['All', ...categories.map(c => {
    const meta = CATEGORY_META[c.name] || { icon: '🔍' }
    return `${meta.icon} ${c.name}`
  })]
  return (
    <div style={{ borderBottom: '1px solid #eee', background: '#fff', position: 'sticky', top: 60, zIndex: 100 }}>
      <div style={{ maxWidth: 1140, margin: '0 auto', padding: '0 20px', display: 'flex', gap: 4, overflowX: 'auto', scrollbarWidth: 'none' }}>
        {tabs.map(t => (
          <div key={t} onClick={() => setActive(t)}
            style={{ flexShrink: 0, padding: '12px 15px', fontSize: 13, fontWeight: 500, cursor: 'pointer', color: active === t ? G : '#666', borderBottom: active === t ? `2px solid ${G}` : '2px solid transparent', whiteSpace: 'nowrap' }}>
            {t}
          </div>
        ))}
      </div>
    </div>
  )
}

// ── PAGES ───────────────────────────────────────────────────

// Trending term → best matching category name
const TREND_CAT = {
  'house cleaning': 'Cleaning', 'lawn care': 'Landscaping', 'plumber': 'Plumbing',
  'electrician': 'Electrical', 'painter': 'Painting', 'snow removal': 'Landscaping',
  'handyman': 'Renovation', 'hvac': 'HVAC', 'moving': 'Moving', 'pet sitting': 'Pet Care',
}

const Home = ({ go, categories, providers, loading, topLocations = [] }) => {
  const [tab, setTab] = useState('All')
  const [searchVal, setSearchVal] = useState('')
  const filtered = tab === 'All' ? providers : providers.filter(p => tab.includes(p.cat))

  const handleSearch = () => {
    const q = searchVal.trim()
    if (!q) { go('/search'); return }
    const matched = Object.entries(TREND_CAT).find(([k]) => q.toLowerCase().includes(k))
    if (matched) go('/search/' + encodeURIComponent(matched[1]))
    else go('/search')
  }

  return (
    <div>
      <div style={{ background: GL, padding: '52px 20px 44px', textAlign: 'center' }}>
        <h1 style={{ fontSize: 'clamp(26px,4vw,44px)', fontWeight: 700, letterSpacing: -1, lineHeight: 1.15, marginBottom: 12 }}>
          Find trusted local <span style={{ color: G }}>service pros</span> near you
        </h1>
        <p style={{ color: '#666', fontSize: 16, marginBottom: 28 }}>Verified, insured, and reviewed — right in your neighbourhood.</p>
        <div style={{ display: 'flex', maxWidth: 600, margin: '0 auto 20px', border: '1.5px solid #ccc', borderRadius: 10, overflow: 'hidden', background: '#fff', boxShadow: '0 2px 10px rgba(0,0,0,0.06)' }}>
          <input
            style={{ flex: 1, border: 'none', outline: 'none', padding: '13px 16px', fontSize: 15 }}
            placeholder="e.g. house cleaning, plumber..."
            value={searchVal}
            onChange={e => setSearchVal(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && handleSearch()}
          />
          <select style={{ border: 'none', borderLeft: '1px solid #eee', outline: 'none', padding: '0 12px', fontSize: 13, color: '#444', background: '#fff', cursor: 'pointer' }}>
            <option value="">All cities</option>
            {topLocations.map(l => <option key={l.location} value={l.location}>{l.location}</option>)}
          </select>
          <button onClick={handleSearch} style={{ background: G, border: 'none', color: '#fff', padding: '0 24px', fontSize: 15, fontWeight: 500, cursor: 'pointer' }}>Search</button>
        </div>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 7, justifyContent: 'center', fontSize: 13 }}>
          <span style={{ color: '#888' }}>Trending:</span>
          {['house cleaning', 'lawn care', 'plumber', 'electrician', 'painter', 'moving', 'handyman'].map(t => (
            <span key={t} onClick={() => go('/search/' + encodeURIComponent(TREND_CAT[t] || 'Other'))}
              style={{ background: '#fff', border: '1px solid #ddd', borderRadius: 20, padding: '4px 12px', fontSize: 12, color: '#444', cursor: 'pointer' }}>{t}</span>
          ))}
        </div>
      </div>
      <CatTabs active={tab} setActive={setTab} categories={categories} />
      <div style={st.wrap}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
          <h2 style={st.h2}>Providers near Cambridge, ON</h2>
          <span onClick={() => go('/search')} style={{ fontSize: 13, color: G, cursor: 'pointer' }}>See all →</span>
        </div>
        {loading ? <Spinner /> : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill,minmax(190px,1fr))', gap: 16, marginBottom: 48 }}>
            {filtered.length > 0
              ? filtered.map(p => <ProviderCard key={p.id} p={p} go={go} />)
              : <p style={st.muted}>No providers found yet. Be the first to list your business!</p>}
          </div>
        )}
        <div style={{ background: GL, borderRadius: 12, padding: '24px 28px', display: 'flex', gap: 28, flexWrap: 'wrap', justifyContent: 'center', marginBottom: 48 }}>
          {[['🪪', 'ID Verified', 'Every provider is identity-checked'], ['📄', 'Certs Reviewed', 'Credentials verified by our team'], ['🛡️', 'Insured', 'Badge-marked liability coverage'], ['⭐', 'Real Reviews', 'From verified customers only']].map(([ic, t, s]) => (
            <div key={t} style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <div style={{ width: 38, height: 38, borderRadius: '50%', background: '#c8e8d9', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 16, flexShrink: 0 }}>{ic}</div>
              <div><div style={{ fontSize: 13, fontWeight: 600 }}>{t}</div><div style={{ fontSize: 12, color: '#777' }}>{s}</div></div>
            </div>
          ))}
        </div>
        <div style={{ border: '1px solid #eee', borderRadius: 14, padding: '32px', display: 'grid', gridTemplateColumns: '1fr auto', gap: 32, alignItems: 'center', marginBottom: 48 }}>
          <div>
            <h2 style={st.h2}>Grow your local service business</h2>
            <p style={{ color: '#666', fontSize: 14, marginBottom: 20, lineHeight: 1.7, maxWidth: 480 }}>StepServe puts your profile in front of homeowners actively searching for what you offer — all for less than a coffee a week.</p>
            {['Full profile with photos & reviews', 'Show up in local search by city & category', 'Display certifications and insurance badges', 'Manage everything from a simple dashboard'].map(b => (
              <div key={b} style={{ display: 'flex', gap: 8, alignItems: 'center', fontSize: 14, marginBottom: 10 }}><span style={{ color: G, fontWeight: 700 }}>✓</span>{b}</div>
            ))}
            <button onClick={() => go('/register')} style={{ ...st.btnG, marginTop: 8, padding: '12px 24px', fontSize: 15 }}>List your business →</button>
          </div>
          <div style={{ background: GL, border: '1.5px solid #b8dfd0', borderRadius: 12, padding: '28px 32px', textAlign: 'center', flexShrink: 0 }}>
            <div style={{ fontSize: 11, color: '#aaa', textTransform: 'uppercase', letterSpacing: 0.5, marginBottom: 8 }}>One simple fee</div>
            <div style={{ fontSize: 46, fontWeight: 800, color: G, letterSpacing: -2, lineHeight: 1 }}><sup style={{ fontSize: 20, verticalAlign: 'top', marginTop: 10, display: 'inline-block' }}>$</sup>5<sub style={{ fontSize: 15, fontWeight: 400, color: '#888' }}>/listing</sub></div>
            <p style={{ fontSize: 13, color: '#888', margin: '8px 0 16px' }}>Free account — pay per listing</p>
            <button onClick={() => go('/register')} style={{ ...st.btnG, width: '100%', padding: 11 }}>Get started</button>
          </div>
        </div>
        <div style={{ background: G, borderRadius: 14, padding: '32px 36px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 24, color: '#fff' }}>
          <div><h3 style={{ ...st.h3, color: '#fff', fontSize: 20, marginBottom: 6 }}>It's easier in the app</h3><p style={{ fontSize: 14, opacity: 0.85 }}>Browse local pros, save favourites, and leave reviews.</p></div>
          <div style={{ display: 'flex', gap: 10, flexShrink: 0 }}>
            {['⬇ App Store', '⬇ Google Play'].map(b => <div key={b} style={{ background: 'rgba(255,255,255,0.15)', border: '1px solid rgba(255,255,255,0.3)', borderRadius: 8, padding: '9px 18px', fontSize: 13, fontWeight: 500, cursor: 'pointer' }}>{b}</div>)}
          </div>
        </div>
      </div>
    </div>
  )
}

// ── Search ──────────────────────────────────────────────────
const Search = ({ go, categories, api, initialCat = 'All' }) => {
  const [tab, setTab] = useState(() => {
    if (!initialCat || initialCat === 'All') return 'All'
    const meta = CATEGORY_META[initialCat] || { icon: '🔍' }
    return `${meta.icon} ${initialCat}`
  })
  const [rating, setRating] = useState('Any')
  const [cert, setCert] = useState(false)
  const [insured, setInsured] = useState(false)
  const [locationFilter, setLocationFilter] = useState('')
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(false)

  const runSearch = async () => {
    setLoading(true)
    try {
      const catName = tab === 'All' ? null : categories.find(c => tab.includes(c.name))?.name
      const catObj = catName ? categories.find(c => c.name === catName) : null
      const raw = await apiSearchServices(api, {
        location: locationFilter || undefined,
        category_id: catObj?.id || undefined,
      })
      let mapped = raw.map(s => serviceToProvider(s, categories))
      if (cert) mapped = mapped.filter(p => p.cert)
      if (insured) mapped = mapped.filter(p => p.insured)
      setResults(mapped)
    } catch {
      setResults([])
    }
    setLoading(false)
  }

  useEffect(() => { runSearch() }, [tab, api])

  return (
    <div>
      <CatTabs active={tab} setActive={setTab} categories={categories} />
      <div style={{ ...st.wrap, display: 'grid', gridTemplateColumns: '220px 1fr', gap: 28, alignItems: 'start' }}>
        <div style={{ ...st.card, position: 'sticky', top: 110 }}>
          <div style={{ fontWeight: 600, marginBottom: 14 }}>Filters</div>
          <div style={{ marginBottom: 16 }}>
            <div style={st.label}>City / Postal code</div>
            <input style={st.input} placeholder="Cambridge, ON" value={locationFilter} onChange={e => setLocationFilter(e.target.value)} />
          </div>
          <div style={{ marginBottom: 16 }}>
            <div style={st.label}>Min. rating</div>
            <select style={st.input} value={rating} onChange={e => setRating(e.target.value)}>
              {['Any', '★★★★★ 5', '★★★★ 4+', '★★★ 3+'].map(o => <option key={o}>{o}</option>)}
            </select>
          </div>
          <div style={{ marginBottom: 10 }}>
            <label style={{ display: 'flex', gap: 8, alignItems: 'center', fontSize: 14, cursor: 'pointer' }}>
              <input type="checkbox" checked={cert} onChange={e => setCert(e.target.checked)} /> Has certification
            </label>
          </div>
          <div style={{ marginBottom: 20 }}>
            <label style={{ display: 'flex', gap: 8, alignItems: 'center', fontSize: 14, cursor: 'pointer' }}>
              <input type="checkbox" checked={insured} onChange={e => setInsured(e.target.checked)} /> Liability insured
            </label>
          </div>
          <button style={{ ...st.btnG, width: '100%' }} onClick={runSearch}>Apply filters</button>
        </div>
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 18 }}>
            <div><span style={{ fontWeight: 600 }}>{results.length} providers</span> <span style={{ color: '#888', fontSize: 14 }}>found</span></div>
            <select style={{ ...st.input, width: 'auto', fontSize: 13, padding: '7px 12px' }}>
              <option>Sort: Top rated</option><option>Sort: Newest</option><option>Sort: Price low</option>
            </select>
          </div>
          {loading ? <Spinner /> : (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill,minmax(185px,1fr))', gap: 16 }}>
              {results.length > 0
                ? results.map(p => <ProviderCard key={p.id} p={p} go={go} />)
                : <p style={st.muted}>No results found. Try adjusting your filters.</p>}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

// ── Provider Profile ────────────────────────────────────────
const ProviderProfile = ({ go, id, providers }) => {
  const p = providers.find(x => x.id === parseInt(id)) || providers[0]
  const reviews = [
    { user: 'Sarah R.', city: 'Toronto', rating: 5, comment: 'Absolutely fantastic service, showed up on time and did a thorough job. Will book again!' },
    { user: 'Michel B.', city: 'Montréal', rating: 5, comment: 'Super professional and friendly. Our house looks brand new.' },
    { user: 'Dave K.', city: 'Calgary', rating: 4, comment: 'Great work, only minor delay on arrival but the quality was excellent.' },
  ]
  if (!p) return <div style={{ ...st.wrap, textAlign: 'center' }}><p style={st.muted}>Provider not found.</p><button onClick={() => go('/search')} style={st.btnG}>Back to search</button></div>
  return (
    <div style={st.wrap}>
      <span onClick={() => go('/search')} style={{ fontSize: 13, color: G, cursor: 'pointer', display: 'block', marginBottom: 16 }}>← Back to search</span>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 300px', gap: 28, alignItems: 'start' }}>
        <div>
          <div style={{ ...st.card, marginBottom: 20 }}>
            <div style={{ display: 'flex', gap: 18, alignItems: 'flex-start' }}>
              <div style={{ width: 88, height: 88, borderRadius: 16, background: p.bg, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 40, flexShrink: 0 }}>{p.icon}</div>
              <div style={{ flex: 1 }}>
                <h1 style={{ ...st.h1, fontSize: 22, marginBottom: 4 }}>{p.name}</h1>
                <div style={{ color: '#888', fontSize: 14, marginBottom: 8 }}>{p.city}</div>
                <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 10 }}>
                  <span style={st.badge()}>{p.cat}</span>
                  {p.cert && <span style={st.badge()}>✓ Certified</span>}
                  {p.insured && <span style={st.badge('#dbeafe', '#1a6eb5')}>✓ Insured</span>}
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}><Stars n={p.rating} /><span style={{ fontSize: 13, color: '#888' }}>({p.reviews} reviews)</span></div>
              </div>
            </div>
          </div>
          <div style={{ ...st.card, marginBottom: 20 }}>
            <h3 style={st.h3}>About</h3>
            <p style={{ fontSize: 14, color: '#555', lineHeight: 1.7 }}>{p.bio}</p>
          </div>
          <div style={{ ...st.card, marginBottom: 20 }}>
            <h3 style={st.h3}>Photo gallery</h3>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4,1fr)', gap: 8 }}>
              {[p.bg, '#f0e8f8', '#e8f0f8', '#f8f0e8'].map((c, i) => (
                <div key={i} style={{ aspectRatio: '1', background: c, borderRadius: 8, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 28 }}>{p.icon}</div>
              ))}
            </div>
          </div>
          <div style={st.card}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
              <h3 style={{ ...st.h3, margin: 0 }}>Reviews ({p.reviews})</h3>
              <button onClick={() => go('/review/' + p.id)} style={st.btnSm}>Leave a review</button>
            </div>
            {reviews.map((r, i) => (
              <div key={i} style={{ borderTop: i > 0 ? '1px solid #f0f0f0' : 'none', paddingTop: i > 0 ? 14 : 0, marginTop: i > 0 ? 14 : 0 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 5 }}>
                  <div style={{ fontWeight: 600, fontSize: 14 }}>{r.user} <span style={{ color: '#aaa', fontWeight: 400, fontSize: 12 }}>· {r.city}</span></div>
                  <Stars n={r.rating} />
                </div>
                <p style={{ fontSize: 14, color: '#555' }}>{r.comment}</p>
              </div>
            ))}
          </div>
        </div>
        <div style={{ position: 'sticky', top: 100 }}>
          <div style={{ ...st.card, marginBottom: 16 }}>
            <div style={{ fontSize: 20, fontWeight: 700, color: G, marginBottom: 4 }}>{p.price}</div>
            <div style={{ fontSize: 13, color: '#888', marginBottom: 16 }}>Contact for a detailed quote</div>
            <button style={{ ...st.btnG, width: '100%', marginBottom: 10, padding: 12 }}>Contact provider</button>
            <button style={{ ...st.btnO, width: '100%', padding: 11 }}>Save to favourites</button>
          </div>
          <div style={{ ...st.card, fontSize: 13 }}>
            <div style={{ fontWeight: 600, marginBottom: 10 }}>Provider details</div>
            {[['Service area', p.city], ['Category', p.cat], ['Rating', `${p.rating}/5 stars`], ['Reviews', p.reviews]].map(([k, v]) => (
              <div key={k} style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid #f5f5f5' }}>
                <span style={{ color: '#888' }}>{k}</span><span style={{ fontWeight: 500 }}>{v}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}

// ── Categories ──────────────────────────────────────────────
const Categories = ({ go, categories }) => (
  <div style={st.wrap}>
    <h1 style={st.h1}>All categories</h1>
    <p style={{ color: '#666', marginBottom: 28 }}>Find verified service professionals in every category across Canada.</p>
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill,minmax(160px,1fr))', gap: 16 }}>
      {categories.map(c => {
        const meta = CATEGORY_META[c.name] || { icon: '🔍' }
        return (
          <div key={c.name} onClick={() => go('/search/' + encodeURIComponent(c.name))}
            style={{ ...st.card, textAlign: 'center', cursor: 'pointer', padding: '28px 16px' }}
            onMouseEnter={e => { e.currentTarget.style.borderColor = G; e.currentTarget.style.boxShadow = `0 2px 12px rgba(10,124,92,0.1)` }}
            onMouseLeave={e => { e.currentTarget.style.borderColor = '#eee'; e.currentTarget.style.boxShadow = 'none' }}>
            <div style={{ fontSize: 36, marginBottom: 10 }}>{meta.icon}</div>
            <div style={{ fontWeight: 600, fontSize: 14, marginBottom: 4 }}>{c.name}</div>
            <div style={{ fontSize: 12, color: '#aaa' }}>{c.services_count} providers</div>
          </div>
        )
      })}
    </div>
  </div>
)

// ── Review ──────────────────────────────────────────────────
const Review = ({ go, id, providers }) => {
  const [stars, setStars] = useState(0)
  const p = providers.find(x => x.id === parseInt(id)) || providers[0]
  return (
    <div style={{ ...st.wrap, maxWidth: 600 }}>
      <span onClick={() => go('/providers/' + (p?.id || ''))} style={{ fontSize: 13, color: G, cursor: 'pointer', display: 'block', marginBottom: 20 }}>← Back to {p?.name}</span>
      <h1 style={st.h1}>Leave a review</h1>
      {p && (
        <div style={{ ...st.card, marginBottom: 16 }}>
          <div style={{ display: 'flex', gap: 14, alignItems: 'center' }}>
            <div style={{ width: 50, height: 50, borderRadius: 10, background: p.bg, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 26 }}>{p.icon}</div>
            <div><div style={{ fontWeight: 600 }}>{p.name}</div><div style={{ fontSize: 13, color: '#888' }}>{p.city}</div></div>
          </div>
        </div>
      )}
      <div style={st.card}>
        <div style={{ marginBottom: 20 }}>
          <div style={{ ...st.label, marginBottom: 10 }}>Your rating</div>
          <div style={{ display: 'flex', gap: 6 }}>
            {[1, 2, 3, 4, 5].map(n => (
              <span key={n} onClick={() => setStars(n)} style={{ fontSize: 34, cursor: 'pointer', color: n <= stars ? '#f5a623' : '#ddd' }}>★</span>
            ))}
          </div>
        </div>
        <div style={{ marginBottom: 16 }}><label style={st.label}>Your name</label><input style={st.input} placeholder="e.g. Sarah R." /></div>
        <div style={{ marginBottom: 16 }}><label style={st.label}>Email (not published)</label><input style={st.input} type="email" placeholder="you@example.com" /></div>
        <div style={{ marginBottom: 20 }}><label style={st.label}>Your review</label><textarea style={{ ...st.input, height: 110, resize: 'vertical' }} placeholder="Share your experience with this provider..." /></div>
        <button style={{ ...st.btnG, width: '100%', padding: 13, fontSize: 15 }}>Submit review</button>
      </div>
    </div>
  )
}

// ── About ───────────────────────────────────────────────────
const About = ({ go }) => (
  <div style={st.wrap}>
    <div style={{ maxWidth: 760, margin: '0 auto' }}>
      <div style={{ textAlign: 'center', marginBottom: 48 }}>
        <h1 style={{ ...st.h1, fontSize: 36 }}>About <span style={{ color: G }}>StepServe</span></h1>
        <p style={{ fontSize: 17, color: '#555', lineHeight: 1.8 }}>We're on a mission to make hiring trusted local service professionals as easy as possible for Canadians.</p>
      </div>
      {[
        ['Our mission', "StepServe was built to solve a simple problem: finding a reliable local tradesperson or service professional should not be stressful. We created a platform where every listed provider is identity-verified, and where customers can review certifications and insurance status before making contact."],
        ['How it works', 'Providers create a free account, then pay a flat $5 CAD per listing to publish their services. Customers browse for free — no account required. Search by category and city, view full profiles, read reviews, and contact providers directly.'],
        ['Our standards', 'Every provider on StepServe goes through an email-verified registration. Certifications and liability insurance documents are uploaded and reviewed by our admin team. Verified credentials are displayed clearly with badges on each profile.'],
        ['Built in Canada', 'StepServe is operated by Kingsman Software Solutions and is fully PIPEDA-compliant. We store your data securely in Canadian data centres and do not sell personal information to third parties.'],
      ].map(([t, c]) => (
        <div key={t} style={{ marginBottom: 32 }}>
          <h2 style={{ ...st.h2, color: G }}>{t}</h2>
          <p style={{ fontSize: 15, color: '#555', lineHeight: 1.8 }}>{c}</p>
        </div>
      ))}
      <div style={{ textAlign: 'center', marginTop: 40 }}>
        <button onClick={() => go('/register')} style={{ ...st.btnG, padding: '14px 32px', fontSize: 16 }}>Join StepServe today</button>
      </div>
    </div>
  </div>
)

// ── Contact ─────────────────────────────────────────────────
const Contact = () => {
  const [form, setForm] = useState({ name: '', email: '', topic: 'General inquiry', message: '' })
  const [sent, setSent] = useState(false)
  const [error, setError] = useState('')

  const handleSend = () => {
    if (!form.name.trim() || !form.email.trim() || !form.message.trim()) {
      setError('Please fill in your name, email, and message.')
      return
    }
    if (!/\S+@\S+\.\S+/.test(form.email)) {
      setError('Please enter a valid email address.')
      return
    }
    setError('')
    setSent(true)
  }

  if (sent) return (
    <div style={{ ...st.wrap, maxWidth: 640, textAlign: 'center', paddingTop: 48 }}>
      <div style={{ fontSize: 52, marginBottom: 16 }}>✅</div>
      <h1 style={st.h1}>Message sent!</h1>
      <p style={{ color: '#666', fontSize: 15, marginBottom: 24 }}>Thanks, {form.name}. We'll get back to you at <strong>{form.email}</strong> within one business day.</p>
      <button onClick={() => { setSent(false); setForm({ name: '', email: '', topic: 'General inquiry', message: '' }) }}
        style={st.btnO}>Send another message</button>
    </div>
  )

  return (
    <div style={{ ...st.wrap, maxWidth: 640 }}>
      <h1 style={st.h1}>Contact us</h1>
      <p style={{ color: '#666', marginBottom: 28 }}>Have a question or need help? Our team usually responds within one business day.</p>
      <div style={st.card}>
        <Banner msg={error} type="error" />
        <div style={{ marginBottom: 14 }}>
          <label style={st.label}>Your name *</label>
          <input style={st.input} placeholder="Full name" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} />
        </div>
        <div style={{ marginBottom: 14 }}>
          <label style={st.label}>Email *</label>
          <input style={st.input} type="email" placeholder="you@example.com" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} />
        </div>
        <div style={{ marginBottom: 14 }}>
          <label style={st.label}>Topic</label>
          <select style={st.input} value={form.topic} onChange={e => setForm({ ...form, topic: e.target.value })}>
            <option>General inquiry</option>
            <option>Provider support</option>
            <option>Billing question</option>
            <option>Report an issue</option>
            <option>Other</option>
          </select>
        </div>
        <div style={{ marginBottom: 20 }}>
          <label style={st.label}>Message *</label>
          <textarea style={{ ...st.input, height: 140, resize: 'vertical' }} placeholder="Describe your question or issue..." value={form.message} onChange={e => setForm({ ...form, message: e.target.value })} />
        </div>
        <button onClick={handleSend} style={{ ...st.btnG, width: '100%', padding: 13, fontSize: 15 }}>Send message</button>
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginTop: 24 }}>
        {[['📧 Email', 'support@stepserve.com'], ['🕐 Response time', 'Within 1 business day']].map(([t, v]) => (
          <div key={t} style={st.card}>
            <div style={{ fontSize: 14, fontWeight: 600, marginBottom: 4 }}>{t}</div>
            <div style={{ fontSize: 13, color: '#888' }}>{v}</div>
          </div>
        ))}
      </div>
    </div>
  )
}

// ── Terms / Privacy ─────────────────────────────────────────
const TextPage = ({ title, sections }) => (
  <div style={{ ...st.wrap, maxWidth: 760 }}>
    <h1 style={st.h1}>{title}</h1>
    <p style={{ fontSize: 13, color: '#aaa', marginBottom: 28 }}>Last updated: April 2026</p>
    {sections.map(([h, c]) => (
      <div key={h} style={{ marginBottom: 24 }}>
        <h3 style={st.h3}>{h}</h3>
        <p style={{ fontSize: 14, color: '#555', lineHeight: 1.8 }}>{c}</p>
      </div>
    ))}
  </div>
)

const Terms = () => <TextPage title="Terms of Service" sections={[
  ['Acceptance', 'By accessing or using StepServe, you agree to be bound by these Terms of Service and all applicable laws and regulations.'],
  ['Provider listings', 'Providers must be legally operating in their province and provide truthful information. StepServe reserves the right to remove any listing at its discretion.'],
  ['Listing fees', 'Publishing a service listing costs $5 CAD (one-time, per listing), processed via Stripe. Fees are non-refundable once a listing is published.'],
  ['Reviews', 'Reviews must be based on genuine experiences. False, defamatory, or spam reviews are prohibited and subject to removal.'],
  ['Liability', 'StepServe is a directory service. We do not employ the providers listed on our platform and are not liable for services rendered.'],
  ['Changes', 'We reserve the right to modify these terms at any time. Continued use of StepServe following changes constitutes acceptance.'],
]} />

const Privacy = () => <TextPage title="Privacy Policy" sections={[
  ['What we collect', 'We collect your name, email address, province, and any information you voluntarily provide when creating a provider profile or leaving a review.'],
  ['How we use it', 'Your information is used to operate the StepServe platform, communicate with you about your account, and improve our services.'],
  ['PIPEDA compliance', 'StepServe complies with the Personal Information Protection and Electronic Documents Act (PIPEDA). You have the right to access and request correction of your personal data.'],
  ['Third parties', 'We use Stripe for payment processing, AWS/Cloudflare for file storage, and SendGrid for transactional email. These services have their own privacy policies.'],
  ['Data retention', 'Your data is retained for as long as your account is active. You may request deletion of your account and personal data at any time by contacting support.'],
  ['Cookies', 'We use minimal cookies for authentication and analytics. You may disable cookies in your browser settings, though some features may not function correctly.'],
]} />

// ── Register (3-step, wired to API) ─────────────────────────
const Register = ({ go, api, onLogin }) => {
  const [step, setStep] = useState(1)
  const [info, setInfo] = useState({ firstName: '', lastName: '', email: '', phone: '', city: '', province: 'ON', password: '' })
  const [profile, setProfile] = useState({ categories: [], bio: '' })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleStep1 = async () => {
    setError('')
    setLoading(true)
    try {
      const res = await apiRegister(api, { email: info.email, password: info.password, role: 'provider' })
      storeToken(res.access_token)
      onLogin(res.access_token)
      setStep(2)
    } catch (err) {
      setError(err?.response?.data?.detail || 'Registration failed. Email may already be in use.')
    }
    setLoading(false)
  }

  const handleStep2 = async () => {
    setError('')
    setLoading(true)
    try {
      const authApi = createApiClient(getStoredToken())
      await apiSaveProviderProfile(authApi, {
        full_name: `${info.firstName} ${info.lastName}`.trim() || info.email,
        bio: profile.bio || null,
        location: info.city ? `${info.city}, ${info.province}` : null,
        hourly_rate: null,
      })
      setStep(3)
    } catch (err) {
      setError(err?.response?.data?.detail || 'Could not save profile. Please try again.')
    }
    setLoading(false)
  }

  return (
    <div style={{ ...st.wrap, maxWidth: 560 }}>
      <div style={{ display: 'flex', gap: 0, marginBottom: 28, borderRadius: 10, overflow: 'hidden', border: '1px solid #eee' }}>
        {['1. Your info', '2. Categories', '3. Payment'].map((s, i) => (
          <div key={s} style={{ flex: 1, padding: '11px', textAlign: 'center', fontSize: 13, fontWeight: step === i + 1 ? 600 : 400, background: step === i + 1 ? G : step > i + 1 ? '#e1f5ee' : '#fafafa', color: step === i + 1 ? '#fff' : step > i + 1 ? G : '#aaa', cursor: step > i + 1 ? 'pointer' : 'default' }} onClick={() => step > i + 1 && setStep(i + 1)}>{s}</div>
        ))}
      </div>
      <Banner msg={error} type="error" />
      {step === 1 && (
        <div style={st.card}>
          <h2 style={st.h2}>Create your provider profile</h2>
          <div style={st.grid2}>
            <div><label style={st.label}>First name</label><input style={st.input} placeholder="John" value={info.firstName} onChange={e => setInfo({ ...info, firstName: e.target.value })} /></div>
            <div><label style={st.label}>Last name</label><input style={st.input} placeholder="Smith" value={info.lastName} onChange={e => setInfo({ ...info, lastName: e.target.value })} /></div>
          </div>
          <div style={{ marginTop: 14 }}><label style={st.label}>Email *</label><input style={st.input} type="email" placeholder="you@example.com" value={info.email} onChange={e => setInfo({ ...info, email: e.target.value })} /></div>
          <div style={{ marginTop: 14 }}><label style={st.label}>Phone</label><input style={st.input} placeholder="+1 (519) 000-0000" value={info.phone} onChange={e => setInfo({ ...info, phone: e.target.value })} /></div>
          <div style={{ marginTop: 14, ...st.grid2 }}>
            <div><label style={st.label}>City</label><input style={st.input} placeholder="Cambridge" value={info.city} onChange={e => setInfo({ ...info, city: e.target.value })} /></div>
            <div><label style={st.label}>Province</label>
              <select style={st.input} value={info.province} onChange={e => setInfo({ ...info, province: e.target.value })}>
                {['ON', 'BC', 'AB', 'QC', 'MB', 'SK', 'NS', 'NB', 'NL', 'PE'].map(p => <option key={p}>{p}</option>)}
              </select>
            </div>
          </div>
          <div style={{ marginTop: 14 }}><label style={st.label}>Password *</label><input style={st.input} type="password" placeholder="Min. 8 characters" value={info.password} onChange={e => setInfo({ ...info, password: e.target.value })} /></div>
          <button onClick={handleStep1} disabled={loading || !info.email || !info.password} style={{ ...st.btnG, width: '100%', padding: 13, marginTop: 20, fontSize: 15, opacity: loading ? 0.7 : 1 }}>
            {loading ? 'Creating account…' : 'Continue →'}
          </button>
          <p style={{ textAlign: 'center', fontSize: 13, color: '#888', marginTop: 12 }}>Already registered? <span onClick={() => go('/login')} style={{ color: G, cursor: 'pointer' }}>Sign in</span></p>
        </div>
      )}
      {step === 2 && (
        <div style={st.card}>
          <h2 style={st.h2}>Select your service categories</h2>
          <p style={{ fontSize: 13, color: '#888', marginBottom: 16 }}>Choose all categories that apply to your business.</p>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, marginBottom: 24 }}>
            {Object.entries(CATEGORY_META).map(([name, meta]) => (
              <label key={name} style={{ display: 'flex', gap: 8, alignItems: 'center', padding: '10px 12px', border: '1.5px solid #eee', borderRadius: 8, cursor: 'pointer', fontSize: 14 }}>
                <input type="checkbox" checked={profile.categories.includes(name)} onChange={e => setProfile(p => ({ ...p, categories: e.target.checked ? [...p.categories, name] : p.categories.filter(c => c !== name) }))} />{meta.icon} {name}
              </label>
            ))}
          </div>
          <div style={{ marginBottom: 14 }}><label style={st.label}>Short bio</label><textarea style={{ ...st.input, height: 90 }} placeholder="Describe your business and services..." value={profile.bio} onChange={e => setProfile({ ...profile, bio: e.target.value })} /></div>
          <div style={{ display: 'flex', gap: 10 }}>
            <button onClick={() => setStep(1)} style={{ ...st.btnO, flex: 1 }}>← Back</button>
            <button onClick={handleStep2} disabled={loading} style={{ ...st.btnG, flex: 2, opacity: loading ? 0.7 : 1 }}>{loading ? 'Saving…' : 'Continue →'}</button>
          </div>
        </div>
      )}
      {step === 3 && (
        <div style={st.card}>
          <h2 style={st.h2}>Your account is ready!</h2>
          <div style={{ background: GL, border: '1px solid #b8dfd0', borderRadius: 10, padding: '18px', marginBottom: 20, textAlign: 'center' }}>
            <div style={{ fontSize: 13, color: '#777', marginBottom: 6, textTransform: 'uppercase', letterSpacing: 0.5 }}>Listing fee</div>
            <div style={{ fontSize: 32, fontWeight: 800, color: G }}>$5<span style={{ fontSize: 16, fontWeight: 400, color: '#888' }}> / listing</span></div>
            <p style={{ fontSize: 13, color: '#777', marginTop: 4 }}>Account signup is free. Pay $5 per listing to publish it live.</p>
          </div>
          {['Free provider account — no monthly fee', 'Pay $5 per listing to go live in search', 'Upload certifications & insurance documents', 'Receive and manage customer bookings', 'Full provider dashboard access'].map(b => (
            <div key={b} style={{ display: 'flex', gap: 8, fontSize: 14, marginBottom: 9 }}><span style={{ color: G }}>✓</span>{b}</div>
          ))}
          <button onClick={() => go('/dashboard')} style={{ ...st.btnG, width: '100%', padding: 13, marginTop: 20, fontSize: 15 }}>Go to Dashboard →</button>
          <p style={{ fontSize: 12, color: '#aaa', textAlign: 'center', marginTop: 10 }}>Listing payments processed securely via Stripe.</p>
        </div>
      )}
    </div>
  )
}

// ── Login ────────────────────────────────────────────────────
const Login = ({ go, api, onLogin }) => {
  const [form, setForm] = useState({ email: '', password: '' })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleLogin = async () => {
    setError('')
    setLoading(true)
    try {
      const res = await apiLogin(api, form)
      storeToken(res.access_token)
      onLogin(res.access_token)
      go('/dashboard')
    } catch (err) {
      setError(err?.response?.data?.detail || 'Invalid email or password.')
    }
    setLoading(false)
  }

  return (
    <div style={{ ...st.wrap, maxWidth: 440 }}>
      <div style={{ textAlign: 'center', marginBottom: 28 }}>
        <h1 style={{ ...st.h1, fontSize: 26 }}>Sign in to StepServe</h1>
        <p style={{ color: '#888', fontSize: 14 }}>Provider accounts only. Customers browse without signing in.</p>
      </div>
      <div style={st.card}>
        <Banner msg={error} type="error" />
        <div style={{ marginBottom: 14 }}><label style={st.label}>Email</label><input style={st.input} type="email" placeholder="you@example.com" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} /></div>
        <div style={{ marginBottom: 20 }}><label style={st.label}>Password</label><input style={st.input} type="password" placeholder="••••••••" value={form.password} onChange={e => setForm({ ...form, password: e.target.value })} onKeyDown={e => e.key === 'Enter' && handleLogin()} /></div>
        <button onClick={handleLogin} disabled={loading || !form.email || !form.password} style={{ ...st.btnG, width: '100%', padding: 13, fontSize: 15, opacity: loading ? 0.7 : 1 }}>
          {loading ? 'Signing in…' : 'Sign in'}
        </button>
        <p style={{ textAlign: 'center', fontSize: 13, color: '#888', marginTop: 14 }}>
          <span style={{ color: G, cursor: 'pointer' }}>Forgot password?</span> · <span onClick={() => go('/register')} style={{ color: G, cursor: 'pointer' }}>Create account</span>
        </p>
      </div>
    </div>
  )
}

// ── Dashboard ────────────────────────────────────────────────
const Dashboard = ({ go, api, currentUser }) => {
  const [dashData, setDashData] = useState({ bookings: [], uploads: [] })
  const [listings, setListings] = useState([])
  const [cats, setCats] = useState([])
  const [profileForm, setProfileForm] = useState({ full_name: '', bio: '', location: '', hourly_rate: '' })
  const [newListing, setNewListing] = useState(null) // null=hidden, {}=open
  const [editingId, setEditingId] = useState(null)
  const [editForm, setEditForm] = useState({})
  const [loading, setLoading] = useState(true)
  const [notice, setNotice] = useState('')
  const [error, setError] = useState('')

  const load = () => {
    if (!api || !currentUser) return
    Promise.all([
      apiGetProviderDashboard(api),
      apiGetProviderListings(api),
      apiGetCategories(api),
    ]).then(([dash, ls, cs]) => {
      setDashData({ bookings: dash.bookings || [], uploads: dash.uploads || [] })
      setListings(ls)
      setCats(cs)
    }).catch(() => setError('Could not load dashboard. Make sure you have a provider account.'))
      .finally(() => setLoading(false))
  }

  useEffect(load, [api, currentUser])

  const saveProfile = async () => {
    setNotice(''); setError('')
    try {
      await apiSaveProviderProfile(api, {
        full_name: profileForm.full_name || currentUser?.email,
        bio: profileForm.bio || null,
        location: profileForm.location || null,
        hourly_rate: profileForm.hourly_rate ? Number(profileForm.hourly_rate) : null,
      })
      setNotice('Profile saved.')
    } catch (err) { setError(err?.response?.data?.detail || 'Could not save profile.') }
  }

  const createListing = async () => {
    setNotice(''); setError('')
    if (!newListing?.title || !newListing?.category_id || !newListing?.price) {
      setError('Title, category and price are required.'); return
    }
    try {
      const svc = await apiCreateService(api, {
        title: newListing.title,
        description: newListing.description || '',
        category_id: Number(newListing.category_id),
        price: Number(newListing.price),
      })
      setListings(prev => [{ ...svc, category_name: cats.find(c => c.id === svc.category_id)?.name || 'Other' }, ...prev])
      setNewListing(null)
      setNotice('Listing created! Pay the $5 listing fee below to publish it.')
    } catch (err) { setError(err?.response?.data?.detail || 'Could not create listing.') }
  }

  const payListing = async (id) => {
    setNotice(''); setError('')
    try {
      await apiPayListing(api, id)
      setListings(prev => prev.map(l => l.id === id ? { ...l, payment_status: 'paid', is_active: 1 } : l))
      setNotice('Payment successful! Your listing is now live.')
    } catch (err) { setError(err?.response?.data?.detail || 'Payment failed.') }
  }

  const saveListing = async (id) => {
    setNotice(''); setError('')
    try {
      const updated = await apiUpdateListing(api, id, {
        title: editForm.title || undefined,
        description: editForm.description || undefined,
        price: editForm.price ? Number(editForm.price) : undefined,
        category_id: editForm.category_id ? Number(editForm.category_id) : undefined,
      })
      setListings(prev => prev.map(l => l.id === id ? {
        ...l, ...updated,
        category_name: cats.find(c => c.id === (updated.category_id || l.category_id))?.name || l.category_name
      } : l))
      setEditingId(null)
      setNotice('Listing updated.')
    } catch (err) { setError(err?.response?.data?.detail || 'Could not update listing.') }
  }

  const deactivate = async (id) => {
    try {
      await apiDeactivateListing(api, id)
      setListings(prev => prev.map(l => l.id === id ? { ...l, is_active: 0 } : l))
    } catch (err) { setError(err?.response?.data?.detail || 'Could not deactivate.') }
  }

  if (loading) return <div style={st.wrap}><Spinner /></div>

  const activeLive = listings.filter(l => l.is_active)
  // Only warn about listings that are both unpaid AND inactive (truly unpublished drafts)
  const pendingPay = listings.filter(l => l.payment_status === 'pending' && !l.is_active)

  return (
    <div style={st.wrap}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 24 }}>
        <div>
          <h1 style={st.h1}>My dashboard</h1>
          <p style={{ color: '#888', fontSize: 14 }}>Welcome, {currentUser?.email}</p>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button onClick={() => go('/dashboard/documents')} style={st.btnO}>Documents</button>
          <button onClick={() => go('/dashboard/billing')} style={st.btnO}>Billing</button>
        </div>
      </div>
      <Banner msg={notice} type="success" />
      <Banner msg={error} type="error" />

      {/* Stats */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4,1fr)', gap: 14, marginBottom: 28 }}>
        {[
          ['Live listings', activeLive.length, 'published'],
          ['Total listings', listings.length, 'all time'],
          ['Bookings', dashData.bookings.length, 'received'],
          ['Uploads', dashData.uploads.length, 'files'],
        ].map(([l, v, s]) => (
          <div key={l} style={{ background: '#f7f7f7', borderRadius: 10, padding: '16px' }}>
            <div style={{ fontSize: 12, color: '#999', marginBottom: 4 }}>{l}</div>
            <div style={{ fontSize: 22, fontWeight: 700, color: '#1a1a1a' }}>{v}</div>
            <div style={{ fontSize: 12, color: '#aaa' }}>{s}</div>
          </div>
        ))}
      </div>

      {/* Listings management */}
      <div style={{ ...st.card, marginBottom: 20 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 18 }}>
          <h3 style={{ ...st.h3, marginBottom: 0 }}>My listings ({listings.length})</h3>
          <button onClick={() => setNewListing({ title: '', description: '', category_id: cats[0]?.id || '', price: '' })}
            style={{ ...st.btnG, fontSize: 13, padding: '8px 16px' }}>+ Add new listing</button>
        </div>

        {/* Add listing form */}
        {newListing && (
          <div style={{ background: GL, border: `1.5px solid #b8dfd0`, borderRadius: 10, padding: 18, marginBottom: 18 }}>
            <div style={{ fontWeight: 600, marginBottom: 14, fontSize: 14 }}>New listing — $5 listing fee to publish</div>
            <div style={st.grid2}>
              <div><label style={st.label}>Title *</label><input style={st.input} placeholder="e.g. House Cleaning" value={newListing.title} onChange={e => setNewListing({ ...newListing, title: e.target.value })} /></div>
              <div><label style={st.label}>Category *</label>
                <select style={st.input} value={newListing.category_id} onChange={e => setNewListing({ ...newListing, category_id: e.target.value })}>
                  {cats.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                </select>
              </div>
            </div>
            <div style={{ marginTop: 12 }}><label style={st.label}>Description</label><textarea style={{ ...st.input, height: 72 }} placeholder="Describe what you offer..." value={newListing.description} onChange={e => setNewListing({ ...newListing, description: e.target.value })} /></div>
            <div style={{ marginTop: 12, maxWidth: 200 }}><label style={st.label}>Your service price ($/visit or /hr) *</label><input style={st.input} type="number" placeholder="e.g. 80" value={newListing.price} onChange={e => setNewListing({ ...newListing, price: e.target.value })} /></div>
            <div style={{ display: 'flex', gap: 8, marginTop: 14 }}>
              <button onClick={createListing} style={st.btnG}>Create listing</button>
              <button onClick={() => setNewListing(null)} style={st.btnO}>Cancel</button>
            </div>
            <p style={{ fontSize: 12, color: '#888', marginTop: 8 }}>Your listing will be created as a draft. A $5 CAD listing fee is charged to publish it live.</p>
          </div>
        )}

        {/* Pending payment notice */}
        {pendingPay.length > 0 && (
          <div style={{ background: '#fffbea', border: '1px solid #f6d860', borderRadius: 8, padding: '10px 14px', marginBottom: 14, fontSize: 13 }}>
            ⚠️ You have {pendingPay.length} unpublished listing{pendingPay.length > 1 ? 's' : ''} awaiting the $5 listing fee.
          </div>
        )}

        {/* Listings table */}
        {listings.length === 0 ? (
          <p style={st.muted}>No listings yet. Add your first listing above to start getting bookings.</p>
        ) : (
          <div>
            {listings.map((l, i) => (
              <div key={l.id} style={{ borderTop: i > 0 ? '1px solid #f0f0f0' : 'none', paddingTop: i > 0 ? 14 : 0, marginTop: i > 0 ? 14 : 0 }}>
                {editingId === l.id ? (
                  /* Edit form */
                  <div style={{ background: '#fafafa', borderRadius: 8, padding: 14 }}>
                    <div style={st.grid2}>
                      <div><label style={st.label}>Title</label><input style={st.input} value={editForm.title} onChange={e => setEditForm({ ...editForm, title: e.target.value })} /></div>
                      <div><label style={st.label}>Category</label>
                        <select style={st.input} value={editForm.category_id} onChange={e => setEditForm({ ...editForm, category_id: e.target.value })}>
                          {cats.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                        </select>
                      </div>
                    </div>
                    <div style={{ marginTop: 10 }}><label style={st.label}>Description</label><textarea style={{ ...st.input, height: 64 }} value={editForm.description} onChange={e => setEditForm({ ...editForm, description: e.target.value })} /></div>
                    <div style={{ marginTop: 10, maxWidth: 180 }}><label style={st.label}>Price ($)</label><input style={st.input} type="number" value={editForm.price} onChange={e => setEditForm({ ...editForm, price: e.target.value })} /></div>
                    <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
                      <button onClick={() => saveListing(l.id)} style={{ ...st.btnG, fontSize: 12, padding: '7px 14px' }}>Save</button>
                      <button onClick={() => setEditingId(null)} style={{ ...st.btnO, fontSize: 12, padding: '7px 14px' }}>Cancel</button>
                    </div>
                  </div>
                ) : (
                  /* Listing row */
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}>
                    <div style={{ flex: 1 }}>
                      <div style={{ fontSize: 14, fontWeight: 600, marginBottom: 3 }}>{l.title}</div>
                      <div style={{ fontSize: 12, color: '#888' }}>
                        {l.category_name} · ${Number(l.price).toFixed(0)}/visit
                        <span style={{ marginLeft: 8, ...st.badge(l.is_active ? '#e1f5ee' : '#f5f5f5', l.is_active ? G : '#888') }}>
                          {l.is_active ? '● Live' : '○ Inactive'}
                        </span>
                        <span style={{ marginLeft: 6, ...st.badge(l.payment_status === 'paid' ? '#e1f5ee' : '#fffbea', l.payment_status === 'paid' ? G : '#b07800') }}>
                          {l.payment_status === 'paid' ? '✓ Paid' : '⏳ Unpaid'}
                        </span>
                      </div>
                      {l.description && <div style={{ fontSize: 12, color: '#aaa', marginTop: 4, maxWidth: 500 }}>{l.description.slice(0, 100)}{l.description.length > 100 ? '…' : ''}</div>}
                    </div>
                    <div style={{ display: 'flex', gap: 6, flexShrink: 0 }}>
                      {l.payment_status !== 'paid' && !l.is_active && (
                        <button onClick={() => payListing(l.id)} style={{ ...st.btnG, fontSize: 12, padding: '6px 12px', background: '#b07800', border: 'none' }}>
                          Pay $5 to publish
                        </button>
                      )}
                      <button onClick={() => { setEditingId(l.id); setEditForm({ title: l.title, description: l.description || '', price: l.price, category_id: cats.find(c => c.name === l.category_name)?.id || '' }) }}
                        style={{ ...st.btnO, fontSize: 12, padding: '6px 12px' }}>Edit</button>
                      {l.is_active === 1 && (
                        <button onClick={() => deactivate(l.id)} style={{ ...st.btnO, fontSize: 12, padding: '6px 12px', color: '#c0392b', borderColor: '#f5c6c6' }}>Deactivate</button>
                      )}
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Profile + bookings */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>
        <div style={st.card}>
          <h3 style={st.h3}>Profile information</h3>
          <div style={{ marginBottom: 12 }}><label style={st.label}>Full name</label><input style={st.input} placeholder="Your full name" value={profileForm.full_name} onChange={e => setProfileForm({ ...profileForm, full_name: e.target.value })} /></div>
          <div style={{ marginBottom: 12 }}><label style={st.label}>Location</label><input style={st.input} placeholder="Cambridge, ON" value={profileForm.location} onChange={e => setProfileForm({ ...profileForm, location: e.target.value })} /></div>
          <div style={{ marginBottom: 12 }}><label style={st.label}>Hourly rate ($)</label><input style={st.input} placeholder="e.g. 75" type="number" value={profileForm.hourly_rate} onChange={e => setProfileForm({ ...profileForm, hourly_rate: e.target.value })} /></div>
          <div style={{ marginBottom: 16 }}><label style={st.label}>Bio</label><textarea style={{ ...st.input, height: 90 }} placeholder="Describe your services..." value={profileForm.bio} onChange={e => setProfileForm({ ...profileForm, bio: e.target.value })} /></div>
          <button onClick={saveProfile} style={{ ...st.btnG, padding: '10px 20px' }}>Save changes</button>
        </div>
        <div style={st.card}>
          <h3 style={st.h3}>Recent bookings ({dashData.bookings.length})</h3>
          {dashData.bookings.length === 0 ? (
            <p style={st.muted}>No bookings yet. Once your listings are live, bookings will appear here.</p>
          ) : (
            dashData.bookings.slice(0, 6).map((b, i) => (
              <div key={b.id} style={{ fontSize: 13, padding: '8px 0', borderBottom: '1px solid #f5f5f5', display: 'flex', justifyContent: 'space-between' }}>
                <span>Booking #{b.id} · <span style={{ color: '#888' }}>{b.status}</span></span>
                <span style={{ fontWeight: 600 }}>${Number(b.total_price).toFixed(2)}</span>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  )
}

// ── DashDocuments ────────────────────────────────────────────
const DashDocuments = ({ api }) => {
  const [uploads, setUploads] = useState([])
  const [uploading, setUploading] = useState(false)
  const [notice, setNotice] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    if (!api) return
    api.get('/providers/uploads').then(r => setUploads(r.data)).catch(() => {})
  }, [api])

  const handleUpload = async (e) => {
    const file = e.target.files[0]
    if (!file) return
    setUploading(true); setNotice(''); setError('')
    try {
      const form = new FormData()
      form.append('file', file)
      await api.post('/providers/uploads', form, { headers: { 'Content-Type': 'multipart/form-data' } })
      setNotice('File uploaded successfully.')
      const r = await api.get('/providers/uploads')
      setUploads(r.data)
    } catch (err) {
      setError(err?.response?.data?.detail || 'Upload failed. Make sure your provider profile is set up first.')
    }
    setUploading(false)
  }

  return (
    <div style={st.wrap}>
      <h1 style={st.h1}>Documents</h1>
      <p style={{ color: '#666', marginBottom: 24 }}>Upload certifications and liability insurance. Our team reviews within 1–2 business days.</p>
      <Banner msg={notice} type="success" />
      <Banner msg={error} type="error" />
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>
        {[{ type: 'Certification', desc: 'Trade certificate, diploma, or professional licence' }, { type: 'Liability Insurance', desc: 'Current certificate of insurance (COI)' }].map(d => (
          <div key={d.type} style={st.card}>
            <h3 style={{ ...st.h3, marginBottom: 8 }}>{d.type}</h3>
            <p style={{ fontSize: 13, color: '#888', marginBottom: 16 }}>{d.desc}</p>
            <label style={{ display: 'block', border: '2px dashed #ddd', borderRadius: 8, padding: '24px', textAlign: 'center', marginBottom: 14, cursor: 'pointer', color: '#aaa' }}>
              <input type="file" style={{ display: 'none' }} accept=".pdf,.jpg,.jpeg,.png" onChange={handleUpload} disabled={uploading} />
              <div style={{ fontSize: 28, marginBottom: 6 }}>📄</div>
              <div style={{ fontSize: 13 }}>{uploading ? 'Uploading…' : 'Click to upload or drag & drop'}</div>
              <div style={{ fontSize: 12, marginTop: 4 }}>PDF, JPG, PNG — max 10MB</div>
            </label>
          </div>
        ))}
      </div>
      {uploads.length > 0 && (
        <div style={{ ...st.card, marginTop: 20 }}>
          <h3 style={st.h3}>Upload history</h3>
          <table style={{ width: '100%', fontSize: 13, borderCollapse: 'collapse' }}>
            <thead><tr style={{ color: '#aaa', textAlign: 'left' }}>{['File', 'Type', 'Size', 'Uploaded'].map(h => <th key={h} style={{ padding: '6px 0', borderBottom: '1px solid #eee', fontWeight: 600 }}>{h}</th>)}</tr></thead>
            <tbody>
              {uploads.map(u => (
                <tr key={u.id}>
                  <td style={{ padding: '8px 0', borderBottom: '1px solid #f5f5f5' }}>{u.file_name}</td>
                  <td style={{ padding: '8px 0', borderBottom: '1px solid #f5f5f5', color: '#888' }}>{u.content_type || '—'}</td>
                  <td style={{ padding: '8px 0', borderBottom: '1px solid #f5f5f5', color: '#888' }}>{u.file_size ? `${(u.file_size / 1024).toFixed(1)} KB` : '—'}</td>
                  <td style={{ padding: '8px 0', borderBottom: '1px solid #f5f5f5', color: '#888' }}>{u.created_at ? new Date(u.created_at).toLocaleDateString() : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

// ── DashBilling ───────────────────────────────────────────────
const DashBilling = ({ api }) => {
  const [payments, setPayments] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!api) return
    apiGetProviderListings(api)
      .then(ls => setPayments(ls.filter(l => l.payment_status === 'paid')))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [api])

  return (
    <div style={{ ...st.wrap, maxWidth: 640 }}>
      <h1 style={st.h1}>Billing</h1>
      <div style={{ ...st.card, marginBottom: 20 }}>
        <h3 style={st.h3}>Listing fee model</h3>
        <p style={{ fontSize: 14, color: '#555', lineHeight: 1.7 }}>
          StepServe charges a flat <strong>$5 CAD per listing</strong> — one-time, no recurring fees.
          Your account is free. You only pay when you want to publish a new listing.
        </p>
      </div>
      <div style={{ ...st.card, marginBottom: 20 }}>
        <h3 style={st.h3}>Paid listings ({payments.length})</h3>
        {loading ? <Spinner /> : payments.length === 0 ? (
          <p style={st.muted}>No paid listings yet.</p>
        ) : (
          <table style={{ width: '100%', fontSize: 13, borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ color: '#aaa', textAlign: 'left' }}>
                {['Listing', 'Category', 'Fee', 'Paid on'].map(h => (
                  <th key={h} style={{ padding: '6px 0', borderBottom: '1px solid #eee', fontWeight: 600 }}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {payments.map(p => (
                <tr key={p.id}>
                  <td style={{ padding: '9px 0', borderBottom: '1px solid #f5f5f5', fontWeight: 500 }}>{p.title}</td>
                  <td style={{ padding: '9px 0', borderBottom: '1px solid #f5f5f5', color: '#888' }}>{p.category_name}</td>
                  <td style={{ padding: '9px 0', borderBottom: '1px solid #f5f5f5', fontWeight: 600, color: G }}>${Number(p.listing_fee || 5).toFixed(2)}</td>
                  <td style={{ padding: '9px 0', borderBottom: '1px solid #f5f5f5', color: '#888' }}>{p.paid_at ? new Date(p.paid_at).toLocaleDateString('en-CA') : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
      <div style={{ ...st.card, background: GL }}>
        <div style={{ fontSize: 13, color: '#555' }}>
          Questions about billing? Email <strong>support@stepserve.com</strong>
        </div>
      </div>
    </div>
  )
}

// ── Admin ─────────────────────────────────────────────────────
const Admin = ({ go, api }) => {
  const [overview, setOverview] = useState(null)
  const [recentUsers, setRecentUsers] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!api) return
    Promise.all([apiAdminOverview(api), apiAdminUsers(api)])
      .then(([ov, users]) => { setOverview(ov); setRecentUsers(users.slice(0, 5)) })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [api])

  if (loading) return <div style={st.wrap}><Spinner /></div>

  return (
    <div style={st.wrap}>
      <h1 style={st.h1}>Admin dashboard</h1>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4,1fr)', gap: 14, marginBottom: 28 }}>
        {[['Total users', overview?.users_count ?? '—'], ['Services', overview?.services_count ?? '—'], ['Bookings', overview?.bookings_count ?? '—'], ['Revenue (paid)', overview ? `$${Number(overview.paid_total).toFixed(2)}` : '—']].map(([l, v]) => (
          <div key={l} style={{ background: '#f7f7f7', borderRadius: 10, padding: '16px' }}>
            <div style={{ fontSize: 12, color: '#999', marginBottom: 4 }}>{l}</div>
            <div style={{ fontSize: 24, fontWeight: 700 }}>{v}</div>
          </div>
        ))}
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>
        <div style={st.card}>
          <h3 style={st.h3}>Recent users</h3>
          {recentUsers.length === 0 ? <p style={st.muted}>No users yet.</p> : recentUsers.map((u, i) => (
            <div key={u.id} onClick={() => go('/admin/providers')} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '10px 0', borderBottom: '1px solid #f5f5f5', cursor: 'pointer' }}>
              <div style={{ width: 34, height: 34, borderRadius: 8, background: GL, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 16, flexShrink: 0 }}>
                {u.role === 'provider' ? '🔧' : u.role === 'admin' ? '🛡️' : '👤'}
              </div>
              <div style={{ flex: 1 }}><div style={{ fontSize: 13, fontWeight: 500 }}>{u.email}</div><div style={{ fontSize: 12, color: '#aaa' }}>{u.role}</div></div>
              <span style={st.badge(u.is_active ? '#e1f5ee' : '#fee', u.is_active ? G : '#c0392b')}>{u.is_active ? 'Active' : 'Inactive'}</span>
            </div>
          ))}
          <div onClick={() => go('/admin/providers')} style={{ fontSize: 13, color: G, cursor: 'pointer', marginTop: 12 }}>Manage all users →</div>
        </div>
        <div style={st.card}>
          <h3 style={st.h3}>Quick links</h3>
          {[['👥 Users', '/admin/providers'], ['📄 Documents', '/admin/documents'], ['🏷️ Categories', '/admin/categories'], ['⭐ Reviews', '/admin/reviews'], ['⚙️ Settings', '/admin/settings']].map(([t, p]) => (
            <div key={t} onClick={() => go(p)} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 0', borderBottom: '1px solid #f5f5f5', cursor: 'pointer', fontSize: 14 }}>
              <span>{t}</span><span style={{ color: '#aaa' }}>→</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

// ── AdminProviders (wired to /admin/users) ────────────────────
const AdminProviders = ({ go, api }) => {
  const [users, setUsers] = useState([])
  const [filter, setFilter] = useState('All')
  const [loading, setLoading] = useState(true)
  const [notice, setNotice] = useState('')

  useEffect(() => {
    if (!api) return
    apiAdminUsers(api).then(setUsers).catch(() => {}).finally(() => setLoading(false))
  }, [api])

  const toggleStatus = async (userId, currentActive) => {
    try {
      await apiAdminUpdateUserStatus(api, userId, !currentActive)
      setUsers(prev => prev.map(u => u.id === userId ? { ...u, is_active: !currentActive ? 1 : 0 } : u))
      setNotice(`User #${userId} ${!currentActive ? 'activated' : 'deactivated'}.`)
    } catch {
      setNotice('Failed to update user status.')
    }
  }

  const displayed = filter === 'All' ? users : users.filter(u => {
    if (filter === 'Active') return u.is_active
    if (filter === 'Inactive') return !u.is_active
    if (filter === 'Providers') return u.role === 'provider'
    if (filter === 'Customers') return u.role === 'customer'
    return true
  })

  return (
    <div style={st.wrap}>
      <h1 style={st.h1}>User management</h1>
      {notice && <Banner msg={notice} type="success" />}
      <div style={{ display: 'flex', gap: 10, marginBottom: 20, flexWrap: 'wrap' }}>
        <input style={{ ...st.input, maxWidth: 260 }} placeholder="Search users..." />
        <select style={{ ...st.input, width: 'auto' }} value={filter} onChange={e => setFilter(e.target.value)}>
          {['All', 'Active', 'Inactive', 'Providers', 'Customers'].map(s => <option key={s}>{s}</option>)}
        </select>
      </div>
      {loading ? <Spinner /> : (
        <div style={{ ...st.card, padding: 0, overflow: 'hidden' }}>
          <table style={{ width: '100%', fontSize: 13, borderCollapse: 'collapse' }}>
            <thead><tr style={{ background: '#f9f9f9' }}>{['ID', 'Email', 'Role', 'Status', 'Joined', 'Actions'].map(h => <th key={h} style={{ padding: '11px 14px', textAlign: 'left', fontWeight: 600, color: '#666', borderBottom: '1px solid #eee' }}>{h}</th>)}</tr></thead>
            <tbody>
              {displayed.map((u, i) => (
                <tr key={u.id} style={{ background: i % 2 ? '#fafafa' : '#fff' }}>
                  <td style={{ padding: '10px 14px', color: '#888' }}>#{u.id}</td>
                  <td style={{ padding: '10px 14px', fontWeight: 500 }}>{u.email}</td>
                  <td style={{ padding: '10px 14px' }}><span style={st.badge(u.role === 'admin' ? '#fdecea' : u.role === 'provider' ? '#e1f5ee' : '#f0f0ff', u.role === 'admin' ? '#c0392b' : u.role === 'provider' ? G : '#555')}>{u.role}</span></td>
                  <td style={{ padding: '10px 14px' }}><span style={st.badge(u.is_active ? '#e1f5ee' : '#fee', u.is_active ? G : '#c0392b')}>{u.is_active ? 'Active' : 'Inactive'}</span></td>
                  <td style={{ padding: '10px 14px', color: '#888' }}>{u.created_at ? new Date(u.created_at).toLocaleDateString() : '—'}</td>
                  <td style={{ padding: '10px 14px' }}>
                    <button onClick={() => toggleStatus(u.id, u.is_active)} style={{ ...st.btnO, padding: '4px 10px', fontSize: 12, color: u.is_active ? '#c0392b' : G, borderColor: u.is_active ? '#f5c6c6' : '#b8dfd0' }}>
                      {u.is_active ? 'Deactivate' : 'Activate'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {displayed.length === 0 && <p style={{ ...st.muted, padding: 20 }}>No users found.</p>}
        </div>
      )}
    </div>
  )
}

// ── AdminDocuments (static) ────────────────────────────────────
const AdminDocuments = () => (
  <div style={st.wrap}>
    <h1 style={st.h1}>Document review</h1>
    <p style={{ color: '#666', marginBottom: 24 }}>Review uploaded certifications and insurance documents from providers.</p>
    {[['ProFinish Painting', 'insurance_coi.pdf', 'Insurance COI', 'Uploaded Apr 14, 2026'], ["Mike's Plumbing", 'trade_cert.pdf', 'Trade Certificate', 'Uploaded Apr 13, 2026']].map(([n, f, t, d]) => (
      <div key={n} style={{ ...st.card, marginBottom: 14 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ flex: 1 }}>
            <div style={{ fontWeight: 600, marginBottom: 3 }}>{n}</div>
            <div style={{ fontSize: 13, color: '#888', marginBottom: 8 }}>{t} · <span style={{ color: G }}>{f}</span> · {d}</div>
            <span style={st.badge('#fef8e6', '#b07800')}>⏳ Pending review</span>
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button style={{ ...st.btnG, fontSize: 12, padding: '8px 14px' }}>✓ Approve</button>
            <button style={{ ...st.btnO, fontSize: 12, color: '#c0392b', borderColor: '#f5c6c6' }}>✕ Reject</button>
          </div>
        </div>
      </div>
    ))}
  </div>
)

// ── AdminCategories (wired to API) ────────────────────────────
const AdminCategories = ({ api }) => {
  const [cats, setCats] = useState([])
  const [adding, setAdding] = useState(false)
  const [newCat, setNewCat] = useState({ name: '', slug: '' })
  const [loading, setLoading] = useState(true)
  const [notice, setNotice] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    if (!api) return
    apiGetCategories(api).then(setCats).catch(() => {}).finally(() => setLoading(false))
  }, [api])

  const createCategory = async () => {
    if (!newCat.name) return
    setNotice(''); setError('')
    try {
      const slug = newCat.slug || newCat.name.toLowerCase().replace(/\s+/g, '-')
      await apiAdminCreateCategory(api, { name: newCat.name, slug })
      const updated = await apiGetCategories(api)
      setCats(updated)
      setNewCat({ name: '', slug: '' })
      setAdding(false)
      setNotice(`Category "${newCat.name}" created.`)
    } catch (err) {
      setError(err?.response?.data?.detail || 'Could not create category.')
    }
  }

  return (
    <div style={st.wrap}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h1 style={{ ...st.h1, marginBottom: 0 }}>Category management</h1>
        <button onClick={() => setAdding(!adding)} style={st.btnG}>+ Add category</button>
      </div>
      <Banner msg={notice} type="success" />
      <Banner msg={error} type="error" />
      {adding && (
        <div style={{ ...st.card, marginBottom: 20, border: `1.5px solid ${G}` }}>
          <h3 style={st.h3}>New category</h3>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 14 }}>
            <div><label style={st.label}>Name</label><input style={st.input} placeholder="e.g. Roofing" value={newCat.name} onChange={e => setNewCat({ ...newCat, name: e.target.value })} /></div>
            <div><label style={st.label}>Slug (auto-generated if empty)</label><input style={st.input} placeholder="roofing" value={newCat.slug} onChange={e => setNewCat({ ...newCat, slug: e.target.value })} /></div>
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button onClick={createCategory} style={st.btnG}>Create category</button>
            <button onClick={() => setAdding(false)} style={st.btnO}>Cancel</button>
          </div>
        </div>
      )}
      {loading ? <Spinner /> : (
        <div style={{ ...st.card, padding: 0, overflow: 'hidden' }}>
          <table style={{ width: '100%', fontSize: 13, borderCollapse: 'collapse' }}>
            <thead><tr style={{ background: '#f9f9f9' }}>{['Icon', 'Name', 'Slug', 'Providers', 'Status'].map(h => <th key={h} style={{ padding: '11px 14px', textAlign: 'left', fontWeight: 600, color: '#666', borderBottom: '1px solid #eee' }}>{h}</th>)}</tr></thead>
            <tbody>
              {cats.map((c, i) => {
                const meta = CATEGORY_META[c.name] || { icon: '🔍' }
                return (
                  <tr key={c.id} style={{ background: i % 2 ? '#fafafa' : '#fff' }}>
                    <td style={{ padding: '10px 14px', fontSize: 20 }}>{meta.icon}</td>
                    <td style={{ padding: '10px 14px', fontWeight: 500 }}>{c.name}</td>
                    <td style={{ padding: '10px 14px', color: '#888', fontFamily: 'monospace' }}>{c.slug}</td>
                    <td style={{ padding: '10px 14px' }}>{c.services_count}</td>
                    <td style={{ padding: '10px 14px' }}><span style={st.badge()}>Active</span></td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

// ── AdminReviews (static) ──────────────────────────────────────
const AdminReviews = () => (
  <div style={st.wrap}>
    <h1 style={st.h1}>Review moderation</h1>
    <div style={{ display: 'flex', gap: 10, marginBottom: 20 }}>
      <input style={{ ...st.input, maxWidth: 260 }} placeholder="Search reviews..." />
      <select style={{ ...st.input, width: 'auto' }}><option>All reviews</option><option>Flagged only</option><option>1-2 stars</option></select>
    </div>
    {[
      { provider: 'Sparkle Clean Co.', user: 'Anonymous', rating: 1, comment: 'Never showed up, total scam!!', flagged: true, date: 'Apr 15, 2026' },
      { provider: "Mike's Plumbing", user: 'Dave K.', rating: 5, comment: 'Mike is fantastic, fixed our drain in under an hour.', flagged: false, date: 'Apr 14, 2026' },
    ].map((r, i) => (
      <div key={i} style={{ ...st.card, marginBottom: 14 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 10 }}>
          <div>
            <span style={{ fontWeight: 600, fontSize: 14 }}>{r.provider}</span>
            <span style={{ color: '#aaa', fontSize: 13 }}> · {r.user} · {r.date}</span>
            {r.flagged && <span style={{ ...st.badge('#fdecea', '#c0392b'), marginLeft: 8 }}>🚩 Flagged</span>}
          </div>
          <Stars n={r.rating} />
        </div>
        <p style={{ fontSize: 14, color: '#555', marginBottom: 12 }}>{r.comment}</p>
        <div style={{ display: 'flex', gap: 8 }}>
          <button style={{ ...st.btnO, fontSize: 12, padding: '6px 12px' }}>Keep review</button>
          <button style={{ ...st.btnO, fontSize: 12, padding: '6px 12px', color: '#c0392b', borderColor: '#f5c6c6' }}>Delete review</button>
        </div>
      </div>
    ))}
  </div>
)

// ── AdminSettings (static) ────────────────────────────────────
const AdminSettings = () => (
  <div style={{ ...st.wrap, maxWidth: 700 }}>
    <h1 style={st.h1}>Site settings</h1>
    {[
      { section: 'Stripe configuration', fields: [['Stripe secret key', 'sk_live_••••••••••••••••', 'password'], ['Stripe publishable key', 'pk_live_••••••••••••••••', 'text'], ['Webhook secret', 'whsec_••••••••••••••', 'password'], ['$5/mo Price ID', 'price_••••••••••••••', 'text']] },
      { section: 'Email (SendGrid)', fields: [['SendGrid API key', 'SG.••••••••••••••', 'password'], ['From address', 'noreply@stepserve.com', 'email'], ['Admin alert email', 'admin@stepserve.com', 'email']] },
    ].map(({ section, fields }) => (
      <div key={section} style={{ ...st.card, marginBottom: 20 }}>
        <h3 style={st.h3}>{section}</h3>
        {fields.map(([label, placeholder, type]) => (
          <div key={label} style={{ marginBottom: 12 }}>
            <label style={st.label}>{label}</label>
            <input style={st.input} type={type} placeholder={placeholder} />
          </div>
        ))}
        <button style={{ ...st.btnG, marginTop: 4 }}>Save {section.split(' ')[0]} settings</button>
      </div>
    ))}
  </div>
)

// ── ROOT APP ─────────────────────────────────────────────────
export default function App() {
  const [route, setRoute] = useState('/')
  const go = (r) => { setRoute(r); window.scrollTo?.(0, 0) }

  // ── Auth state ──────────────────────────────────────────────
  const [token, setToken] = useState(() => getStoredToken())
  const [currentUser, setCurrentUser] = useState(null)

  // ── Data state ──────────────────────────────────────────────
  const [categories, setCategories] = useState([])
  const [providers, setProviders] = useState([])
  const [topLocations, setTopLocations] = useState([])
  const [homeLoading, setHomeLoading] = useState(false)

  // ── API client (recreated when token changes) ───────────────
  const api = useMemo(() => createApiClient(token), [token])

  // ── On mount: load home + restore session ───────────────────
  useEffect(() => {
    setHomeLoading(true)
    // Load categories and home data
    Promise.all([
      apiGetCategories(api),
      apiGetHome(api),
    ]).then(([cats, home]) => {
      setCategories(cats)
      setTopLocations(home.top_locations || [])
      const mapped = [...(home.featured || []), ...(home.latest || [])]
        .filter((s, i, arr) => arr.findIndex(x => x.id === s.id) === i) // dedupe
        .map(s => serviceToProvider(s, cats))
      setProviders(mapped)
    }).catch(() => {
      // Backend not available — empty state, no mock data
    }).finally(() => setHomeLoading(false))
  }, [])

  // ── Restore user session from stored token ──────────────────
  useEffect(() => {
    if (!token) { setCurrentUser(null); return }
    apiGetMe(api).then(setCurrentUser).catch(() => {
      clearToken(); setToken(''); setCurrentUser(null)
    })
  }, [token])

  // ── Auth callbacks ──────────────────────────────────────────
  const onLogin = (newToken) => {
    storeToken(newToken)
    setToken(newToken)
  }
  const onLogout = () => {
    clearToken(); setToken(''); setCurrentUser(null); go('/')
  }

  const isAdmin = route.startsWith('/admin')

  const renderPage = () => {
    const common = { go, categories, providers, api, currentUser }

    if (route === '/') return <Home go={go} categories={categories} providers={providers} loading={homeLoading} topLocations={topLocations} />
    if (route === '/search' || route.startsWith('/search/')) {
      const cat = route.startsWith('/search/') ? decodeURIComponent(route.split('/search/')[1]) : 'All'
      return <Search go={go} categories={categories} api={api} initialCat={cat} />
    }
    if (route.startsWith('/providers/')) return <ProviderProfile go={go} id={route.split('/')[2]} providers={providers} />
    if (route === '/categories') return <Categories go={go} categories={categories} />
    if (route.startsWith('/review/')) return <Review go={go} id={route.split('/')[2]} providers={providers} />
    if (route === '/about') return <About go={go} />
    if (route === '/contact') return <Contact />
    if (route === '/terms') return <Terms />
    if (route === '/privacy') return <Privacy />
    if (route === '/register') return <Register go={go} api={api} onLogin={onLogin} />
    if (route === '/login') return <Login go={go} api={api} onLogin={onLogin} />
    if (route === '/dashboard') return <Dashboard go={go} api={api} currentUser={currentUser} />
    if (route === '/dashboard/documents') return <DashDocuments api={api} />
    if (route === '/dashboard/billing') return <DashBilling api={api} />
    if (route === '/admin') return <Admin go={go} api={api} />
    if (route === '/admin/providers') return <AdminProviders go={go} api={api} />
    if (route === '/admin/documents') return <AdminDocuments />
    if (route === '/admin/categories') return <AdminCategories api={api} />
    if (route === '/admin/reviews') return <AdminReviews />
    if (route === '/admin/settings') return <AdminSettings />
    return (
      <div style={{ ...st.wrap, textAlign: 'center', paddingTop: 80 }}>
        <div style={{ fontSize: 48, marginBottom: 16 }}>🔍</div>
        <h1 style={st.h1}>404 — Page not found</h1>
        <p style={{ color: '#888', marginBottom: 24 }}>The page you're looking for doesn't exist.</p>
        <button onClick={() => go('/')} style={st.btnG}>Go home</button>
      </div>
    )
  }

  return (
    <div style={{ minHeight: '100vh', background: '#fff', fontFamily: '-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif', color: '#1a1a1a', lineHeight: 1.5 }}>
      <Nav route={route} go={go} currentUser={currentUser} onLogout={onLogout} />
      <div style={{ minHeight: '60vh' }}>{renderPage()}</div>
      {!isAdmin && <Footer go={go} />}
    </div>
  )
}
