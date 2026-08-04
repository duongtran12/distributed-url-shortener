import { HealthBadge, type HealthState } from '../components/HealthBadge'
import type { UserProfile } from '../auth/types'

interface DashboardHomeProps {
  user: UserProfile
  health: HealthState
  onLogout: () => void
}

export function DashboardHome({ user, health, onLogout }: DashboardHomeProps) {
  return (
    <main className="dashboard-shell">
      <aside className="dashboard-sidebar">
        <a className="brand" href="/" aria-label="Shortwave dashboard"><span className="brand-mark">S</span><span>shortwave</span></a>
        <nav className="dashboard-nav" aria-label="Dashboard navigation">
          <a className="active" href="#overview">Overview</a>
          <a href="#links">Links</a>
          <a href="#analytics">Analytics</a>
        </nav>
        <button className="logout-button" type="button" onClick={onLogout}>Sign out</button>
      </aside>
      <section className="dashboard-content">
        <header className="dashboard-header">
          <div><p className="eyebrow"><span /> Workspace</p><h1>Good to see you,<br /><em>{user.displayName}.</em></h1></div>
          <div className="dashboard-identity"><HealthBadge health={health} /><span>{user.email}</span></div>
        </header>
        <div className="dashboard-empty" id="overview">
          <span className="capability-index">NEXT / 01</span>
          <h2>Your secure workspace is ready.</h2>
          <p>Authentication is connected to the Spring Boot API. Link management and live analytics arrive in the next step.</p>
          <button className="primary-button" type="button">Create a short link <span>-&gt;</span></button>
        </div>
      </section>
    </main>
  )
}
