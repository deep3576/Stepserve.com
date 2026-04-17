import { useState } from 'react'
import logo from '../assets/logo.svg'
import logoDark from '../assets/logo-dark.svg'

const G = '#0a7c5c'
const GD = '#085e47'

export default function Nav({ route, go, currentUser, onLogout }) {
  const [menuOpen, setMenuOpen] = useState(false)
  const isAdmin = route.startsWith('/admin')
  const isDash = route.startsWith('/dashboard')

  // Navigate and close mobile menu
  const nav = (path) => { go(path); setMenuOpen(false) }

  // ── Admin nav ─────────────────────────────────────────────
  if (isAdmin) {
    const links = [
      ['Providers', '/admin/providers'],
      ['Documents', '/admin/documents'],
      ['Categories', '/admin/categories'],
      ['Reviews', '/admin/reviews'],
      ['Settings', '/admin/settings'],
    ]
    return (
      <>
        <div style={{ background: '#1a1a1a', padding: '0 20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', height: 52, position: 'sticky', top: 0, zIndex: 200 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 24 }}>
            <div onClick={() => nav('/admin')} style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0 }}>
              <img src={logoDark} alt="StepServe" style={{ height: 28 }} />
              <span style={{ fontSize: 11, color: '#555', fontWeight: 400 }}>Admin</span>
            </div>
            <div className="nav-desktop-links" style={{ gap: 24 }}>
              {links.map(([t, p]) => (
                <div key={t} onClick={() => nav(p)}
                  style={{ color: route === p ? '#4ade80' : '#999', fontSize: 13, cursor: 'pointer', fontWeight: route === p ? 600 : 400, transition: 'color 0.15s' }}
                  onMouseEnter={e => { if (route !== p) e.currentTarget.style.color = '#ccc' }}
                  onMouseLeave={e => { if (route !== p) e.currentTarget.style.color = '#999' }}>
                  {t}
                </div>
              ))}
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <div className="nav-desktop-auth" style={{ alignItems: 'center' }}>
              <div onClick={() => nav('/')} style={{ color: '#666', fontSize: 12, cursor: 'pointer' }}>← Exit admin</div>
            </div>
            <button className="nav-burger" onClick={() => setMenuOpen(o => !o)} style={{ color: '#aaa' }}>
              {menuOpen ? '✕' : '☰'}
            </button>
          </div>
        </div>
        {menuOpen && (
          <div style={{ position: 'fixed', top: 52, left: 0, right: 0, bottom: 0, background: '#111', zIndex: 199, padding: '20px', overflowY: 'auto' }}>
            {links.map(([t, p]) => (
              <div key={t} onClick={() => nav(p)}
                style={{ padding: '14px 0', borderBottom: '1px solid #222', fontSize: 16, color: route === p ? '#4ade80' : '#ccc', fontWeight: route === p ? 600 : 400, cursor: 'pointer' }}>
                {t}
              </div>
            ))}
            <div onClick={() => nav('/')} style={{ padding: '14px 0', fontSize: 16, color: '#888', cursor: 'pointer', marginTop: 8 }}>
              ← Exit admin
            </div>
          </div>
        )}
      </>
    )
  }

  // ── Dashboard nav ─────────────────────────────────────────
  if (isDash) {
    const links = [
      ['Overview', '/dashboard'],
      ['Documents', '/dashboard/documents'],
      ['Billing', '/dashboard/billing'],
    ]
    return (
      <>
        <div style={{ background: '#fff', borderBottom: '1px solid #eee', padding: '0 20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', height: 60, position: 'sticky', top: 0, zIndex: 200 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
            <img src={logo} alt="StepServe" onClick={() => nav('/')} style={{ height: 32, cursor: 'pointer', marginRight: 12 }} />
            <div className="nav-desktop-links">
              {links.map(([t, p]) => (
                <div key={t} onClick={() => nav(p)}
                  style={{ fontSize: 13, fontWeight: 500, cursor: 'pointer', color: route === p ? G : '#666', borderBottom: route === p ? `2px solid ${G}` : '2px solid transparent', padding: '19px 12px', transition: 'color 0.15s' }}>
                  {t}
                </div>
              ))}
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <div className="nav-desktop-auth" style={{ gap: 12, alignItems: 'center' }}>
              <div onClick={() => nav('/')} style={{ fontSize: 13, color: '#666', cursor: 'pointer' }}>← Public site</div>
              <div onClick={() => nav('/admin')} style={{ fontSize: 13, color: G, cursor: 'pointer', fontWeight: 500 }}>Admin →</div>
            </div>
            <button className="nav-burger" onClick={() => setMenuOpen(o => !o)}>
              {menuOpen ? '✕' : '☰'}
            </button>
          </div>
        </div>
        {menuOpen && (
          <div style={{ position: 'fixed', top: 60, left: 0, right: 0, bottom: 0, background: '#fff', zIndex: 199, padding: '20px', borderTop: '1px solid #eee', overflowY: 'auto' }}>
            {links.map(([t, p]) => (
              <div key={t} onClick={() => nav(p)}
                style={{ padding: '14px 0', borderBottom: '1px solid #f0f0f0', fontSize: 16, color: route === p ? G : '#333', fontWeight: route === p ? 600 : 400, cursor: 'pointer' }}>
                {t}
              </div>
            ))}
            <div style={{ marginTop: 8 }}>
              <div onClick={() => nav('/')} style={{ padding: '14px 0', borderBottom: '1px solid #f0f0f0', fontSize: 16, color: '#666', cursor: 'pointer' }}>← Public site</div>
              <div onClick={() => nav('/admin')} style={{ padding: '14px 0', fontSize: 16, color: G, cursor: 'pointer', fontWeight: 500 }}>Admin →</div>
            </div>
          </div>
        )}
      </>
    )
  }

  // ── Public nav ────────────────────────────────────────────
  const publicLinks = [
    ['Browse', '/search'],
    ['Categories', '/categories'],
    ['About', '/about'],
    ['Contact', '/contact'],
  ]
  return (
    <>
      <div style={{ background: '#fff', borderBottom: '1px solid #eee', padding: '0 20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', height: 60, position: 'sticky', top: 0, zIndex: 200 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <img src={logo} alt="StepServe" onClick={() => nav('/')} style={{ height: 34, cursor: 'pointer', flexShrink: 0 }} />
          <div className="nav-desktop-links" style={{ alignItems: 'center', gap: 2 }}>
            {publicLinks.map(([t, p]) => (
              <div key={t} onClick={() => nav(p)}
                style={{ fontSize: 14, fontWeight: 500, cursor: 'pointer', color: route === p ? G : '#555', padding: '8px 12px', borderRadius: 8, background: route === p ? '#f0faf6' : 'transparent', transition: 'all 0.15s' }}
                onMouseEnter={e => { if (route !== p) { e.currentTarget.style.color = G; e.currentTarget.style.background = '#f9fffe' } }}
                onMouseLeave={e => { if (route !== p) { e.currentTarget.style.color = '#555'; e.currentTarget.style.background = 'transparent' } }}>
                {t}
              </div>
            ))}
          </div>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <div className="nav-desktop-auth" style={{ alignItems: 'center', gap: 8 }}>
            {currentUser ? (
              <>
                <div onClick={() => nav(currentUser.role === 'admin' ? '/admin' : '/dashboard')}
                  style={{ fontSize: 13, color: '#555', cursor: 'pointer', padding: '8px 10px', borderRadius: 8, maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {currentUser.email}
                </div>
                <div onClick={onLogout}
                  style={{ fontSize: 14, fontWeight: 500, cursor: 'pointer', color: '#555', padding: '8px 14px', borderRadius: 8, border: '1.5px solid #ddd', transition: 'all 0.15s', whiteSpace: 'nowrap' }}
                  onMouseEnter={e => { e.currentTarget.style.color = '#c0392b'; e.currentTarget.style.borderColor = '#f5c6c6' }}
                  onMouseLeave={e => { e.currentTarget.style.color = '#555'; e.currentTarget.style.borderColor = '#ddd' }}>
                  Sign out
                </div>
              </>
            ) : (
              <>
                <div onClick={() => nav('/login')}
                  style={{ fontSize: 14, fontWeight: 500, cursor: 'pointer', color: '#555', padding: '8px 14px', borderRadius: 8, transition: 'all 0.15s', whiteSpace: 'nowrap' }}
                  onMouseEnter={e => { e.currentTarget.style.color = G; e.currentTarget.style.background = '#f0faf6' }}
                  onMouseLeave={e => { e.currentTarget.style.color = '#555'; e.currentTarget.style.background = 'transparent' }}>
                  Sign in
                </div>
                <div onClick={() => nav('/register')}
                  style={{ fontSize: 14, fontWeight: 600, cursor: 'pointer', background: G, color: '#fff', padding: '9px 16px', borderRadius: 8, transition: 'background 0.15s', whiteSpace: 'nowrap' }}
                  onMouseEnter={e => e.currentTarget.style.background = GD}
                  onMouseLeave={e => e.currentTarget.style.background = G}>
                  List your business
                </div>
              </>
            )}
          </div>
          <button className="nav-burger" onClick={() => setMenuOpen(o => !o)}>
            {menuOpen ? '✕' : '☰'}
          </button>
        </div>
      </div>

      {/* Mobile menu overlay */}
      {menuOpen && (
        <div style={{ position: 'fixed', top: 60, left: 0, right: 0, bottom: 0, background: '#fff', zIndex: 199, padding: '12px 20px 32px', borderTop: '1px solid #eee', overflowY: 'auto' }}>
          {publicLinks.map(([t, p]) => (
            <div key={t} onClick={() => nav(p)}
              style={{ padding: '15px 0', borderBottom: '1px solid #f0f0f0', fontSize: 16, color: route === p ? G : '#222', fontWeight: route === p ? 600 : 400, cursor: 'pointer' }}>
              {t}
            </div>
          ))}
          <div style={{ paddingTop: 16 }}>
            {currentUser ? (
              <>
                <div style={{ fontSize: 13, color: '#aaa', padding: '8px 0' }}>{currentUser.email}</div>
                <div onClick={() => nav(currentUser.role === 'admin' ? '/admin' : '/dashboard')}
                  style={{ padding: '14px 0', borderBottom: '1px solid #f0f0f0', fontSize: 16, color: G, cursor: 'pointer', fontWeight: 500 }}>
                  Dashboard
                </div>
                <div onClick={() => { onLogout(); setMenuOpen(false) }}
                  style={{ padding: '14px 0', fontSize: 16, color: '#c0392b', cursor: 'pointer', fontWeight: 500, marginTop: 4 }}>
                  Sign out
                </div>
              </>
            ) : (
              <>
                <div onClick={() => nav('/login')}
                  style={{ padding: '14px 0', borderBottom: '1px solid #f0f0f0', fontSize: 16, color: '#222', cursor: 'pointer', fontWeight: 500 }}>
                  Sign in
                </div>
                <div onClick={() => nav('/register')}
                  style={{ display: 'block', marginTop: 14, padding: '14px 20px', background: G, color: '#fff', borderRadius: 8, fontSize: 15, cursor: 'pointer', fontWeight: 600, textAlign: 'center' }}>
                  List your business
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </>
  )
}
