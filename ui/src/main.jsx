import React from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import './styles.css';

const G = '#0a7c5c'

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props)
    this.state = { error: null }
  }

  static getDerivedStateFromError(error) {
    return { error }
  }

  componentDidCatch(error, info) {
    console.error('StepServe ErrorBoundary caught:', error, info)
  }

  render() {
    if (this.state.error) {
      return (
        <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', fontFamily: '-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif', background: '#fff' }}>
          <div style={{ textAlign: 'center', maxWidth: 420, padding: '0 20px' }}>
            <div style={{ fontSize: 48, marginBottom: 16 }}>⚠️</div>
            <h2 style={{ fontSize: 22, fontWeight: 700, color: '#1a1a1a', marginBottom: 8 }}>Something went wrong</h2>
            <p style={{ color: '#888', fontSize: 14, marginBottom: 24 }}>An unexpected error occurred. Please refresh the page to continue.</p>
            <button
              onClick={() => window.location.reload()}
              style={{ background: G, color: '#fff', border: 'none', borderRadius: 8, padding: '11px 24px', fontSize: 14, fontWeight: 500, cursor: 'pointer' }}>
              Refresh page
            </button>
          </div>
        </div>
      )
    }
    return this.props.children
  }
}

createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <ErrorBoundary>
      <App />
    </ErrorBoundary>
  </React.StrictMode>
);
