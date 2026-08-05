import { useEffect, useState } from 'react'
import { AuthPanel } from './auth/AuthPanel'
import { clearAccessToken, getAccessToken, getCurrentUser } from './auth/authApi'
import type { UserProfile } from './auth/types'
import { DashboardHome } from './dashboard/DashboardHome'
import { HealthBadge, type HealthState } from './components/HealthBadge'

type AuthView = 'login' | 'register' | null

const platformFeatures = [
  { index: '01', title: 'Reliable redirects', detail: 'Redis caching keeps frequently used routes responsive while PostgreSQL remains the source of truth.' },
  { index: '02', title: 'Asynchronous tracking', detail: 'Click events move through RabbitMQ so analytics processing never blocks a visitor redirect.' },
  { index: '03', title: 'Actionable analytics', detail: 'Understand traffic trends, unique visitors, devices, referrers, browsers, and operating systems.' },
]

function ArrowIcon() {
  return <span aria-hidden="true">-&gt;</span>
}

function App() {
  const [health, setHealth] = useState<HealthState>('checking')
  const [authView, setAuthView] = useState<AuthView>(null)
  const [user, setUser] = useState<UserProfile | null>(null)
  const [sessionLoading, setSessionLoading] = useState(() => Boolean(getAccessToken()))

  useEffect(() => {
    const controller = new AbortController()
    fetch(`${import.meta.env.VITE_API_BASE_URL || ''}/actuator/health`, {
      signal: controller.signal,
    })
      .then((response) => {
        if (!response.ok) throw new Error('Backend unavailable')
        return response.json() as Promise<{ status?: string }>
      })
      .then((payload) => setHealth(payload.status === 'UP' ? 'online' : 'offline'))
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === 'AbortError') return
        setHealth('offline')
      })

    return () => controller.abort()
  }, [])

  useEffect(() => {
    if (!getAccessToken()) return
    getCurrentUser()
      .then(setUser)
      .catch(() => clearAccessToken())
      .finally(() => setSessionLoading(false))
  }, [])

  function handleAuthenticated(profile: UserProfile) {
    setUser(profile)
    setAuthView(null)
  }

  function handleLogout() {
    clearAccessToken()
    setUser(null)
  }

  if (sessionLoading) {
    return (
      <main className="session-loader">
        <span className="brand-mark">S</span>
        <p>Restoring secure session...</p>
      </main>
    )
  }

  if (user) {
    return <DashboardHome user={user} health={health} onLogout={handleLogout} />
  }

  return (
    <main className="site-shell">
      <nav className="topbar" aria-label="Primary navigation">
        <a className="brand" href="#top" aria-label="Shortwave home">
          <span className="brand-mark">S</span>
          <span>shortwave</span>
        </a>
        <div className="nav-actions">
          <button className="text-button" type="button" onClick={() => setAuthView('login')}>
            Sign in
          </button>
          <button className="primary-button primary-button--small" type="button" onClick={() => setAuthView('register')}>
            Get started <ArrowIcon />
          </button>
        </div>
      </nav>

      <section className="hero" id="top">
        <div className="hero-copy">
          <p className="eyebrow"><span /> Link management and analytics</p>
          <h1>Shorten links.<br /><em>Understand every click.</em></h1>
          <p className="hero-description">
            Create reliable short URLs, control every destination, and turn live traffic into
            useful insight from one focused workspace.
          </p>
          <div className="hero-actions">
            <button className="primary-button" type="button" onClick={() => setAuthView('register')}>
              Create an account <ArrowIcon />
            </button>
            <button className="secondary-button" type="button" onClick={() => setAuthView('login')}>Sign in to your workspace</button>
          </div>
          <div className="hero-assurance" aria-label="Platform capabilities">
            <span>Custom aliases</span><span>Private analytics</span><span>Distributed infrastructure</span>
          </div>
        </div>

        <aside className="workflow-card" id="workflow" aria-label="How Shortwave works">
          <div className="workflow-card__header">
            <div><span className="muted-label">A simpler workflow</span><h2>From long URL to clear signal.</h2></div>
            <HealthBadge health={health} />
          </div>
          <ol className="workflow-list">
            <li><span>01</span><div><strong>Create</strong><p>Choose a generated code or a memorable custom alias.</p></div></li>
            <li><span>02</span><div><strong>Share</strong><p>Route visitors through a cached, resilient redirect path.</p></div></li>
            <li><span>03</span><div><strong>Learn</strong><p>Inspect traffic trends without storing raw visitor addresses.</p></div></li>
          </ol>
          <div className="workflow-route" aria-label="Example short URL">
            <span className="health-dot" />
            <code>shortwave / product-launch</code>
            <span>Ready</span>
          </div>
        </aside>
      </section>

      <section className="platform-features" aria-label="Platform features">
        {platformFeatures.map((item) => (
          <article className="platform-feature" key={item.title}>
            <span className="capability-index">{item.index}</span>
            <div><h2>{item.title}</h2><p>{item.detail}</p></div>
          </article>
        ))}
      </section>

      {authView && (
        <AuthPanel
          initialMode={authView}
          onAuthenticated={handleAuthenticated}
          onClose={() => setAuthView(null)}
        />
      )}
    </main>
  )
}

export default App
