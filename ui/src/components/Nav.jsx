const G = '#0a7c5c'
const GD = '#085e47'

export default function Nav({ route, go, currentUser, onLogout }) {
  const isAdmin = route.startsWith('/admin')
  const isDash = route.startsWith('/dashboard')

  if (isAdmin) {
    return (
      <div style={{ background: '#1a1a1a', padding: '0 20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', height: 52, position: 'sticky', top: 0, zIndex: 200 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 28 }}>
          <div onClick={() => go('/admin')} style={{ color: '#fff', fontWeight: 800, fontSize: 15, cursor: 'pointer', letterSpacing: -0.3 }}>
            <span style={{ color: '#4ade80' }}>Step</span>Serve <span style={{ fontSize: 11, color: '#666', fontWeight: 400, marginLeft: 4 }}>Admin</span>
          </div>
          {[['Providers', '/admin/providers'], ['Documents', '/admin/documents'], ['Categories', '/admin/categories'], ['Reviews', '/admin/reviews'], ['Settings', '/admin/settings']].map(([t, p]) => (
            <div key={t} onClick={() => go(p)}
              style={{ color: route === p ? '#4ade80' : '#999', fontSize: 13, cursor: 'pointer', fontWeight: route === p ? 600 : 400, transition: 'color 0.15s' }}
              onMouseEnter={e => { if (route !== p) e.currentTarget.style.color = '#ccc' }}
              onMouseLeave={e => { if (route !== p) e.currentTarget.style.color = '#999' }}>
              {t}
            </div>
          ))}
        </div>
        <div onClick={() => go('/')} style={{ color: '#666', fontSize: 12, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 4 }}>
          ← Exit admin
        </div>
      </div>
    )
  }

  if (isDash) {
    return (
      <div style={{ background: '#fff', borderBottom: '1px solid #eee', padding: '0 20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', height: 60, position: 'sticky', top: 0, zIndex: 200 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 28 }}>
          <div onClick={() => go('/')} style={{ fontWeight: 800, fontSize: 17, color: G, cursor: 'pointer', letterSpacing: -0.5 }}>StepServe</div>
          {[['Overview', '/dashboard'], ['Documents', '/dashboard/documents'], ['Billing', '/dashboard/billing']].map(([t, p]) => (
            <div key={t} onClick={() => go(p)}
              style={{ fontSize: 13, fontWeight: 500, cursor: 'pointer', color: route === p ? G : '#666', borderBottom: route === p ? `2px solid ${G}` : '2px solid transparent', padding: '19px 0', transition: 'color 0.15s' }}>
              {t}
            </div>
          ))}
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <div onClick={() => go('/')} style={{ fontSize: 13, color: '#666', cursor: 'pointer' }}>← Public site</div>
          <div onClick={() => go('/admin')} style={{ fontSize: 13, color: G, cursor: 'pointer', fontWeight: 500 }}>Admin →</div>
        </div>
      </div>
    )
  }

  // Public nav
  return (
    <div style={{ background: '#fff', borderBottom: '1px solid #eee', padding: '0 20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', height: 60, position: 'sticky', top: 0, zIndex: 200 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 32 }}>
        <div onClick={() => go('/')} style={{ fontWeight: 800, fontSize: 18, color: G, cursor: 'pointer', letterSpacing: -0.5 }}>StepServe</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
          {[['Browse', '/search'], ['Categories', '/categories'], ['About', '/about'], ['Contact', '/contact']].map(([t, p]) => (
            <div key={t} onClick={() => go(p)}
              style={{ fontSize: 14, fontWeight: 500, cursor: 'pointer', color: route === p ? G : '#555', padding: '8px 12px', borderRadius: 8, background: route === p ? '#f0faf6' : 'transparent', transition: 'all 0.15s' }}
              onMouseEnter={e => { if (route !== p) { e.currentTarget.style.color = G; e.currentTarget.style.background = '#f9fffe' } }}
              onMouseLeave={e => { if (route !== p) { e.currentTarget.style.color = '#555'; e.currentTarget.style.background = 'transparent' } }}>
              {t}
            </div>
          ))}
        </div>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        {currentUser ? (
          <>
            <div onClick={() => go(currentUser.role === 'admin' ? '/admin' : '/dashboard')}
              style={{ fontSize: 13, color: '#555', cursor: 'pointer', padding: '8px 12px', borderRadius: 8 }}>
              {currentUser.email}
            </div>
            <div onClick={onLogout}
              style={{ fontSize: 14, fontWeight: 500, cursor: 'pointer', color: '#555', padding: '8px 14px', borderRadius: 8, border: '1.5px solid #ddd', transition: 'all 0.15s' }}
              onMouseEnter={e => { e.currentTarget.style.color = '#c0392b'; e.currentTarget.style.borderColor = '#f5c6c6' }}
              onMouseLeave={e => { e.currentTarget.style.color = '#555'; e.currentTarget.style.borderColor = '#ddd' }}>
              Sign out
            </div>
          </>
        ) : (
          <>
            <div onClick={() => go('/login')}
              style={{ fontSize: 14, fontWeight: 500, cursor: 'pointer', color: '#555', padding: '8px 14px', borderRadius: 8, transition: 'all 0.15s' }}
              onMouseEnter={e => { e.currentTarget.style.color = G; e.currentTarget.style.background = '#f0faf6' }}
              onMouseLeave={e => { e.currentTarget.style.color = '#555'; e.currentTarget.style.background = 'transparent' }}>
              Sign in
            </div>
            <div onClick={() => go('/register')}
              style={{ fontSize: 14, fontWeight: 600, cursor: 'pointer', background: G, color: '#fff', padding: '9px 18px', borderRadius: 8, transition: 'background 0.15s' }}
              onMouseEnter={e => e.currentTarget.style.background = GD}
              onMouseLeave={e => e.currentTarget.style.background = G}>
              List your business
            </div>
          </>
        )}
      </div>
    </div>
  )
}
