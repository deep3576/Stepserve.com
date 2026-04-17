import logoDark from '../assets/logo-dark.svg'

export default function Footer({ go }) {
  return (
    <footer style={{ background: '#111', color: '#aaa', padding: '48px 20px 28px', marginTop: 48 }}>
      <div style={{ maxWidth: 1140, margin: '0 auto' }}>
        <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1fr 1fr', gap: 40, marginBottom: 40 }}>
          {/* Brand */}
          <div>
            <img src={logoDark} alt="StepServe" onClick={() => go('/')} style={{ height: 36, cursor: 'pointer', marginBottom: 12 }} />
            <p style={{ fontSize: 14, lineHeight: 1.7, color: '#888', maxWidth: 280, margin: '0 0 16px' }}>
              Find trusted local service professionals across Canada. Verified, insured, and reviewed.
            </p>
            <div style={{ fontSize: 13, color: '#666' }}>
              Operated by Kingsman Software Solutions<br />
              Cambridge, ON · Canada
            </div>
          </div>

          {/* For Customers */}
          <div>
            <div style={{ fontSize: 12, fontWeight: 700, color: '#555', textTransform: 'uppercase', letterSpacing: 0.8, marginBottom: 14 }}>For Customers</div>
            {[['Browse Providers', '/search'], ['All Categories', '/categories'], ['How it Works', '/about'], ['Leave a Review', '/search']].map(([t, p]) => (
              <div key={t} onClick={() => go(p)}
                style={{ fontSize: 14, color: '#888', cursor: 'pointer', marginBottom: 9, transition: 'color 0.15s' }}
                onMouseEnter={e => e.currentTarget.style.color = '#ccc'}
                onMouseLeave={e => e.currentTarget.style.color = '#888'}>
                {t}
              </div>
            ))}
          </div>

          {/* For Providers */}
          <div>
            <div style={{ fontSize: 12, fontWeight: 700, color: '#555', textTransform: 'uppercase', letterSpacing: 0.8, marginBottom: 14 }}>For Providers</div>
            {[['List Your Business', '/register'], ['Provider Dashboard', '/dashboard'], ['Pricing', '/register'], ['Upload Documents', '/dashboard/documents']].map(([t, p]) => (
              <div key={t} onClick={() => go(p)}
                style={{ fontSize: 14, color: '#888', cursor: 'pointer', marginBottom: 9, transition: 'color 0.15s' }}
                onMouseEnter={e => e.currentTarget.style.color = '#ccc'}
                onMouseLeave={e => e.currentTarget.style.color = '#888'}>
                {t}
              </div>
            ))}
          </div>

          {/* Company */}
          <div>
            <div style={{ fontSize: 12, fontWeight: 700, color: '#555', textTransform: 'uppercase', letterSpacing: 0.8, marginBottom: 14 }}>Company</div>
            {[['About StepServe', '/about'], ['Contact Us', '/contact'], ['Terms of Service', '/terms'], ['Privacy Policy', '/privacy']].map(([t, p]) => (
              <div key={t} onClick={() => go(p)}
                style={{ fontSize: 14, color: '#888', cursor: 'pointer', marginBottom: 9, transition: 'color 0.15s' }}
                onMouseEnter={e => e.currentTarget.style.color = '#ccc'}
                onMouseLeave={e => e.currentTarget.style.color = '#888'}>
                {t}
              </div>
            ))}
          </div>
        </div>

        {/* Bottom bar */}
        <div style={{ borderTop: '1px solid #222', paddingTop: 20, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
          <div style={{ fontSize: 13, color: '#555' }}>
            © {new Date().getFullYear()} StepServe · Kingsman Software Solutions. All rights reserved.
          </div>
          <div style={{ display: 'flex', gap: 20 }}>
            {[['Terms', '/terms'], ['Privacy', '/privacy'], ['Contact', '/contact']].map(([t, p]) => (
              <div key={t} onClick={() => go(p)}
                style={{ fontSize: 13, color: '#555', cursor: 'pointer', transition: 'color 0.15s' }}
                onMouseEnter={e => e.currentTarget.style.color = '#888'}
                onMouseLeave={e => e.currentTarget.style.color = '#555'}>
                {t}
              </div>
            ))}
          </div>
        </div>
      </div>
    </footer>
  )
}
