import { useCallback, useEffect, useMemo, useState } from 'react'
import { ApiClientError } from '../auth/authApi'
import { AccountSettingsPanel } from '../auth/AccountSettingsPanel'
import type { UserProfile } from '../auth/types'
import { getAnalyticsOverview } from '../analytics/analyticsApi'
import { TrafficChart } from '../analytics/TrafficChart'
import { UrlAnalyticsPanel } from '../analytics/UrlAnalyticsPanel'
import type { AnalyticsOverview } from '../analytics/types'
import { HealthBadge, type HealthState } from '../components/HealthBadge'
import { CreateLinkForm } from '../links/CreateLinkForm'
import { LinkCard } from '../links/LinkCard'
import { getShortUrls } from '../links/linkApi'
import type { ShortUrl, ShortUrlPage } from '../links/types'

interface DashboardHomeProps {
  user: UserProfile
  health: HealthState
  onLogout: () => void
  onPasswordChanged: () => void
}

const EMPTY_PAGE: ShortUrlPage = {
  content: [], page: 0, size: 20, totalElements: 0, totalPages: 0,
}

export function DashboardHome({ user, health, onLogout, onPasswordChanged }: DashboardHomeProps) {
  const [links, setLinks] = useState<ShortUrlPage>(EMPTY_PAGE)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
	const [showCreate, setShowCreate] = useState(false)
	const [analytics, setAnalytics] = useState<AnalyticsOverview | null>(null)
	const [analyticsDays, setAnalyticsDays] = useState(30)
	const [analyticsLoading, setAnalyticsLoading] = useState(true)
	const [analyticsError, setAnalyticsError] = useState('')
	const [analyticsRefresh, setAnalyticsRefresh] = useState(0)
	const [selectedAnalyticsLink, setSelectedAnalyticsLink] = useState<ShortUrl | null>(null)
	const [showAccountSettings, setShowAccountSettings] = useState(false)

  const loadPage = useCallback(async (page: number) => {
    setLoading(true)
    setError('')
    try {
      setLinks(await getShortUrls(page))
    } catch (caught: unknown) {
      setError(caught instanceof ApiClientError ? caught.message : 'Could not load your links.')
    } finally {
      setLoading(false)
    }
  }, [])

	useEffect(() => {
    let active = true
    getShortUrls(0)
      .then((page) => { if (active) setLinks(page) })
      .catch((caught: unknown) => {
        if (active) setError(caught instanceof ApiClientError ? caught.message : 'Could not load your links.')
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
	}, [])

	useEffect(() => {
		let active = true
		getAnalyticsOverview(analyticsDays)
			.then((overview) => { if (active) setAnalytics(overview) })
			.catch((caught: unknown) => {
				if (active) setAnalyticsError(caught instanceof ApiClientError ? caught.message : 'Could not load analytics.')
			})
			.finally(() => { if (active) setAnalyticsLoading(false) })
		return () => { active = false }
	}, [analyticsDays, analyticsRefresh])

  const pageClicks = useMemo(
    () => links.content.reduce((total, link) => total + link.clickCount, 0),
    [links.content],
  )
  const activeLinks = useMemo(
    () => links.content.filter((link) => link.status === 'ACTIVE' && (!link.expiresAt || new Date(link.expiresAt) > new Date())).length,
    [links.content],
  )

	function handleCreated(created: ShortUrl) {
    setShowCreate(false)
    setLinks((current) => ({
      ...current,
      content: [created, ...current.content].slice(0, current.size),
      totalElements: current.totalElements + 1,
      totalPages: Math.ceil((current.totalElements + 1) / current.size),
		}))
		refreshAnalytics()
  }

	function handleUpdated(updated: ShortUrl) {
    setLinks((current) => ({
      ...current,
      content: current.content.map((link) => link.id === updated.id ? updated : link),
		}))
		refreshAnalytics()
  }

	function handleDeleted(id: number) {
    setLinks((current) => ({
      ...current,
      content: current.content.filter((link) => link.id !== id),
      totalElements: Math.max(0, current.totalElements - 1),
      totalPages: Math.ceil(Math.max(0, current.totalElements - 1) / current.size),
		}))
		refreshAnalytics()
	}

	function refreshAnalytics() {
		setAnalyticsLoading(true)
		setAnalyticsError('')
		setAnalyticsRefresh((current) => current + 1)
	}

	function changeAnalyticsRange(days: number) {
		if (days === analyticsDays) return
		setAnalyticsLoading(true)
		setAnalyticsError('')
		setAnalyticsDays(days)
	}

  return (
    <main className="dashboard-shell">
      <aside className="dashboard-sidebar">
        <a className="brand" href="/" aria-label="Shortwave dashboard"><span className="brand-mark">S</span><span>shortwave</span></a>
        <nav className="dashboard-nav" aria-label="Dashboard navigation">
          <a href="#overview">Workspace</a>
          <a href="#analytics">Traffic</a>
          <a href="#links">Short links</a>
        </nav>
        <div className="sidebar-account-actions">
          <button className="settings-button" type="button" onClick={() => setShowAccountSettings(true)}>Account settings</button>
          <button className="logout-button" type="button" onClick={onLogout}>Sign out</button>
        </div>
      </aside>

      <section className="dashboard-content">
        <header className="workspace-header" id="overview">
          <div><p className="eyebrow"><span /> Workspace overview</p><h1>Welcome, {user.displayName}</h1><p>Create, manage, and understand every short link from one place.</p></div>
          <div className="dashboard-identity"><HealthBadge health={health} /><span>{user.email}</span></div>
        </header>

		<section className="summary-grid summary-grid--analytics" aria-label="Account analytics summary">
		  <article><span>Total links</span><strong>{(analytics?.totalUrls ?? links.totalElements).toLocaleString()}</strong></article>
		  <article><span>Active links</span><strong>{(analytics?.activeUrls ?? activeLinks).toLocaleString()}</strong></article>
		  <article><span>Period clicks</span><strong>{(analytics?.periodClicks ?? pageClicks).toLocaleString()}</strong></article>
		  <article><span>Unique visitors</span><strong>{(analytics?.periodUniqueVisitors ?? 0).toLocaleString()}</strong></article>
		</section>

		<section className="analytics-section" id="analytics">
		  <div className="section-heading analytics-heading">
			<div><span className="capability-index">TRAFFIC SIGNAL</span><h2>Click activity</h2></div>
			<div className="range-picker" aria-label="Analytics date range">
			  {[7, 30, 90].map((days) => <button key={days} className={analyticsDays === days ? 'active' : ''} type="button" onClick={() => changeAnalyticsRange(days)}>{days}D</button>)}
			</div>
		  </div>
		  {analyticsError ? <div className="auth-error links-load-error">{analyticsError}<button type="button" onClick={refreshAnalytics}>Retry</button></div> : (
			<div className={`analytics-panel ${analyticsLoading ? 'analytics-panel--loading' : ''}`}>
			  <div className="analytics-total"><span>Lifetime clicks</span><strong>{(analytics?.lifetimeClicks ?? 0).toLocaleString()}</strong><small>{analytics?.from} / {analytics?.to}</small></div>
			  <TrafficChart data={analytics?.dailyClicks ?? []} />
			</div>
		  )}
		  {analytics && analytics.topUrls.length > 0 && (
			<div className="top-links">
			  <div className="top-links-title"><span>Top routes</span><span>Clicks / Visitors</span></div>
			  {analytics.topUrls.slice(0, 5).map((link, index) => (
				<div className="top-link-row" key={link.shortUrlId}><span className="capability-index">0{index + 1}</span><div><strong>/{link.shortCode}</strong><small>{link.originalUrl}</small></div><span>{link.clicks.toLocaleString()} / {link.uniqueVisitors.toLocaleString()}</span></div>
			  ))}
			</div>
		  )}
		</section>

        <section className="links-section" id="links">
          <div className="section-heading">
            <div><span className="capability-index">ROUTES</span><h2>Your short links</h2></div>
            <button className="primary-button" type="button" onClick={() => setShowCreate((visible) => !visible)}>{showCreate ? 'Close form' : 'Create link'} <span>{showCreate ? 'x' : '+'}</span></button>
          </div>

          {showCreate && <CreateLinkForm onCreated={handleCreated} onCancel={() => setShowCreate(false)} />}
          {error && <div className="auth-error links-load-error" role="alert">{error} <button type="button" onClick={() => void loadPage(links.page)}>Try again</button></div>}

          {loading ? (
            <div className="links-state"><span className="health-dot" /> Loading secure links...</div>
          ) : links.content.length === 0 ? (
            <div className="dashboard-empty">
              <span className="capability-index">EMPTY / 00</span>
              <h2>No links yet.</h2>
              <p>Create your first short URL. Click events and analytics will appear here automatically.</p>
              <button className="primary-button" type="button" onClick={() => setShowCreate(true)}>Create a short link <span>-&gt;</span></button>
            </div>
          ) : (
            <div className="links-list">
			  {links.content.map((link) => <LinkCard key={link.id} link={link} onUpdated={handleUpdated} onDeleted={handleDeleted} onViewAnalytics={setSelectedAnalyticsLink} />)}
            </div>
          )}

          {links.totalPages > 1 && (
            <nav className="pagination" aria-label="Link pages">
              <button type="button" disabled={links.page === 0 || loading} onClick={() => void loadPage(links.page - 1)}>Previous</button>
              <span>Page {links.page + 1} of {links.totalPages}</span>
              <button type="button" disabled={links.page + 1 >= links.totalPages || loading} onClick={() => void loadPage(links.page + 1)}>Next</button>
            </nav>
          )}
		</section>
		{selectedAnalyticsLink && <UrlAnalyticsPanel link={selectedAnalyticsLink} onClose={() => setSelectedAnalyticsLink(null)} />}
		{showAccountSettings && <AccountSettingsPanel user={user} onClose={() => setShowAccountSettings(false)} onPasswordChanged={onPasswordChanged} />}
      </section>
    </main>
  )
}
