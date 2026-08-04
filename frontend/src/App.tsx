import { useEffect, useState } from 'react'

type HealthState = 'checking' | 'online' | 'offline'

const capabilities = [
  { label: 'Fast redirects', detail: 'Redis-backed resolution', value: '< 20 ms' },
  { label: 'Click pipeline', detail: 'RabbitMQ event delivery', value: 'Async' },
  { label: 'Traffic guard', detail: 'Distributed rate limiting', value: 'Active' },
]

function ArrowIcon() {
  return <span aria-hidden="true">↗</span>
}

function App() {
  const [health, setHealth] = useState<HealthState>('checking')

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

  return (
    <main className="site-shell">
      <nav className="topbar" aria-label="Primary navigation">
        <a className="brand" href="#top" aria-label="Shortwave home">
          <span className="brand-mark">S</span>
          <span>shortwave</span>
        </a>
        <div className="nav-actions">
          <span className={`health health--${health}`}>
            <span className="health-dot" />
            {health === 'checking' ? 'Checking API' : health === 'online' ? 'API online' : 'API offline'}
          </span>
          <button className="text-button" type="button">Sign in</button>
          <button className="primary-button primary-button--small" type="button">
            Open dashboard <ArrowIcon />
          </button>
        </div>
      </nav>

      <section className="hero" id="top">
        <div className="hero-copy">
          <p className="eyebrow"><span /> Distributed by design</p>
          <h1>Short links.<br /><em>Clear signals.</em></h1>
          <p className="hero-description">
            Create resilient short links and understand every click through a privacy-first,
            real-time analytics platform.
          </p>
          <div className="hero-actions">
            <button className="primary-button" type="button">Create your first link <ArrowIcon /></button>
            <a className="secondary-link" href="#architecture">Explore the architecture <span>↓</span></a>
          </div>
        </div>

        <div className="signal-card" aria-label="Live traffic preview">
          <div className="signal-card__top">
            <div>
              <span className="muted-label">Live traffic</span>
              <strong>2,847</strong>
            </div>
            <span className="trend">+18.4%</span>
          </div>
          <div className="chart" aria-hidden="true">
            <svg viewBox="0 0 560 210" preserveAspectRatio="none">
              <defs>
                <linearGradient id="chart-fill" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#50e3a4" stopOpacity="0.32" />
                  <stop offset="100%" stopColor="#50e3a4" stopOpacity="0" />
                </linearGradient>
              </defs>
              <path className="chart-area" d="M0 178 C46 172 62 126 108 139 S176 168 213 109 S278 82 314 118 S369 144 402 76 S474 28 560 45 L560 210 L0 210 Z" />
              <path className="chart-line" d="M0 178 C46 172 62 126 108 139 S176 168 213 109 S278 82 314 118 S369 144 402 76 S474 28 560 45" />
            </svg>
          </div>
          <div className="signal-footer">
            <span><i className="pulse" /> Events streaming</span>
            <span>Last 24 hours</span>
          </div>
        </div>
      </section>

      <section className="capabilities" id="architecture" aria-label="Platform capabilities">
        {capabilities.map((item, index) => (
          <article className="capability" key={item.label}>
            <span className="capability-index">0{index + 1}</span>
            <div>
              <h2>{item.label}</h2>
              <p>{item.detail}</p>
            </div>
            <strong>{item.value}</strong>
          </article>
        ))}
      </section>
    </main>
  )
}

export default App
