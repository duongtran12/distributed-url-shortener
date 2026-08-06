import { type FormEvent, useCallback, useEffect, useMemo, useState } from 'react'
import { ApiClientError } from '../auth/authApi'
import { AccountSettingsPanel } from '../auth/AccountSettingsPanel'
import type { UserProfile } from '../auth/types'
import { getAnalyticsCsv, getAnalyticsOverview } from '../analytics/analyticsApi'
import { TrafficChart } from '../analytics/TrafficChart'
import { UrlAnalyticsPanel } from '../analytics/UrlAnalyticsPanel'
import type { AnalyticsOverview } from '../analytics/types'
import { HealthBadge, type HealthState } from '../components/HealthBadge'
import { CreateLinkForm } from '../links/CreateLinkForm'
import { LinkCard } from '../links/LinkCard'
import { bulkUpdateShortUrls, getShortUrls, type BulkShortUrlAction, type ShortUrlSort } from '../links/linkApi'
import type { ShortUrl, ShortUrlPage, ShortUrlStatus } from '../links/types'

interface DashboardHomeProps {
  user: UserProfile
  health: HealthState
  onLogout: () => void
  onPasswordChanged: () => void
  onProfileUpdated: (profile: UserProfile) => void
}

const EMPTY_PAGE: ShortUrlPage = {
  content: [], page: 0, size: 20, totalElements: 0, totalPages: 0,
}

export function DashboardHome({ user, health, onLogout, onPasswordChanged, onProfileUpdated }: DashboardHomeProps) {
  const [links, setLinks] = useState<ShortUrlPage>(EMPTY_PAGE)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
	const [showCreate, setShowCreate] = useState(false)
	const [analytics, setAnalytics] = useState<AnalyticsOverview | null>(null)
	const [analyticsDays, setAnalyticsDays] = useState(30)
	const [analyticsLoading, setAnalyticsLoading] = useState(true)
	const [analyticsError, setAnalyticsError] = useState('')
	const [analyticsRefresh, setAnalyticsRefresh] = useState(0)
	const [exportingAnalytics, setExportingAnalytics] = useState(false)
	const [exportError, setExportError] = useState('')
	const [selectedAnalyticsLink, setSelectedAnalyticsLink] = useState<ShortUrl | null>(null)
	const [showAccountSettings, setShowAccountSettings] = useState(false)
	const [searchInput, setSearchInput] = useState('')
	const [searchQuery, setSearchQuery] = useState('')
	const [statusFilter, setStatusFilter] = useState<ShortUrlStatus | ''>('')
	const [linkSort, setLinkSort] = useState<ShortUrlSort>('NEWEST')
	const [selectedLinkIds, setSelectedLinkIds] = useState<Set<number>>(() => new Set())
	const [bulkWorking, setBulkWorking] = useState(false)
	const [bulkError, setBulkError] = useState('')

  const loadPage = useCallback(async (page: number) => {
    setLoading(true)
    setError('')
	setSelectedLinkIds(new Set())
    try {
      setLinks(await getShortUrls(page, 20, {
		query: searchQuery || undefined,
		status: statusFilter || undefined,
		sort: linkSort,
	  }))
    } catch (caught: unknown) {
      setError(caught instanceof ApiClientError ? caught.message : 'Could not load your links.')
    } finally {
      setLoading(false)
    }
  }, [searchQuery, statusFilter, linkSort])

	useEffect(() => {
		let active = true
		getShortUrls(0, 20, {
			query: searchQuery || undefined,
			status: statusFilter || undefined,
			sort: linkSort,
		})
			.then((page) => { if (active) setLinks(page) })
			.catch((caught: unknown) => {
				if (active) setError(caught instanceof ApiClientError ? caught.message : 'Could not load your links.')
			})
			.finally(() => { if (active) setLoading(false) })
		return () => { active = false }
	}, [searchQuery, statusFilter, linkSort])

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

	function handleCreated() {
    setShowCreate(false)
		void loadPage(0)
		refreshAnalytics()
  }

	function handleUpdated() {
		void loadPage(links.page)
		refreshAnalytics()
  }

	function handleDeleted() {
		const targetPage = links.content.length === 1 && links.page > 0 ? links.page - 1 : links.page
		void loadPage(targetPage)
		refreshAnalytics()
	}

	function handleSearch(event: FormEvent<HTMLFormElement>) {
		event.preventDefault()
		const nextQuery = searchInput.trim()
		if (nextQuery === searchQuery) {
			void loadPage(0)
			return
		}
		setLoading(true)
		setError('')
		setSelectedLinkIds(new Set())
		setSearchQuery(nextQuery)
	}

	function changeStatusFilter(status: ShortUrlStatus | '') {
		setLoading(true)
		setError('')
		setSelectedLinkIds(new Set())
		setStatusFilter(status)
	}

	function changeLinkSort(sort: ShortUrlSort) {
		setLoading(true)
		setError('')
		setSelectedLinkIds(new Set())
		setLinkSort(sort)
	}

	function clearFilters() {
		const alreadyClear = !searchInput && !searchQuery && !statusFilter
		if (!alreadyClear) {
			setLoading(true)
			setError('')
		}
		setSearchInput('')
		setSearchQuery('')
		setStatusFilter('')
		setSelectedLinkIds(new Set())
		if (alreadyClear) void loadPage(0)
	}

	function refreshAnalytics() {
		setAnalyticsLoading(true)
		setAnalyticsError('')
		setAnalyticsRefresh((current) => current + 1)
	}

	function changeLinkSelection(id: number, selected: boolean) {
		setSelectedLinkIds((current) => {
			const next = new Set(current)
			if (selected) next.add(id)
			else next.delete(id)
			return next
		})
	}

	function toggleCurrentPageSelection() {
		const allSelected = links.content.every((link) => selectedLinkIds.has(link.id))
		setSelectedLinkIds(allSelected ? new Set() : new Set(links.content.map((link) => link.id)))
	}

	async function runBulkAction(action: BulkShortUrlAction) {
		const ids = [...selectedLinkIds]
		if (ids.length === 0) return
		if (action === 'DELETE' && !window.confirm(`Delete ${ids.length} selected links? This cannot be undone.`)) return
		setBulkWorking(true)
		setBulkError('')
		try {
			await bulkUpdateShortUrls(ids, action)
			setSelectedLinkIds(new Set())
			await loadPage(action === 'DELETE' && links.content.length === ids.length && links.page > 0 ? links.page - 1 : links.page)
			refreshAnalytics()
		} catch (caught: unknown) {
			setBulkError(caught instanceof ApiClientError ? caught.message : 'Could not update the selected links.')
		} finally {
			setBulkWorking(false)
		}
	}

	function changeAnalyticsRange(days: number) {
		if (days === analyticsDays) return
		setAnalyticsLoading(true)
		setAnalyticsError('')
		setAnalyticsDays(days)
	}

	async function exportAnalytics() {
		setExportingAnalytics(true)
		setExportError('')
		try {
			const { blob, filename } = await getAnalyticsCsv(analyticsDays)
			const downloadUrl = URL.createObjectURL(blob)
			const link = document.createElement('a')
			link.href = downloadUrl
			link.download = filename
			document.body.appendChild(link)
			link.click()
			link.remove()
			URL.revokeObjectURL(downloadUrl)
		} catch (caught: unknown) {
			setExportError(caught instanceof ApiClientError ? caught.message : 'Could not export analytics.')
		} finally {
			setExportingAnalytics(false)
		}
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
			<div className="analytics-controls">
			  <div className="range-picker" aria-label="Analytics date range">
				{[7, 30, 90].map((days) => <button key={days} className={analyticsDays === days ? 'active' : ''} type="button" onClick={() => changeAnalyticsRange(days)}>{days}D</button>)}
			  </div>
			  <button className="export-button" type="button" disabled={exportingAnalytics} onClick={() => void exportAnalytics()}>{exportingAnalytics ? 'Exporting...' : 'Export CSV'}</button>
			</div>
		  </div>
		  {exportError && <div className="export-error" role="alert">{exportError}</div>}
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
          <form className="link-filters" onSubmit={handleSearch}>
			<label className="link-search">
			  <span>Search routes</span>
			  <input value={searchInput} onChange={(event) => setSearchInput(event.target.value)} maxLength={200} placeholder="Title, short code, or destination URL" />
			</label>
			<button className="filter-submit" type="submit" disabled={loading}>Search</button>
			<label className="status-filter">
			  <span>Status</span>
			  <select value={statusFilter} onChange={(event) => changeStatusFilter(event.target.value as ShortUrlStatus | '')}>
				<option value="">All statuses</option>
				<option value="ACTIVE">Active</option>
				<option value="DISABLED">Disabled</option>
				<option value="BLOCKED">Blocked</option>
			  </select>
			</label>
			<label className="sort-filter">
			  <span>Sort by</span>
			  <select value={linkSort} onChange={(event) => changeLinkSort(event.target.value as ShortUrlSort)}>
				<option value="NEWEST">Newest</option>
				<option value="OLDEST">Oldest</option>
				<option value="MOST_CLICKED">Most clicked</option>
			  </select>
			</label>
			{(searchQuery || statusFilter) && <button className="clear-filters" type="button" onClick={clearFilters}>Clear filters</button>}
		  </form>
          {error && <div className="auth-error links-load-error" role="alert">{error} <button type="button" onClick={() => void loadPage(links.page)}>Try again</button></div>}
		  {links.content.length > 0 && (
			<div className="bulk-toolbar">
			  <label><input type="checkbox" checked={links.content.every((link) => selectedLinkIds.has(link.id))} onChange={toggleCurrentPageSelection} /><span>Select page</span></label>
			  <strong>{selectedLinkIds.size} selected</strong>
			  {selectedLinkIds.size > 0 && <div className="bulk-actions">
				<button type="button" disabled={bulkWorking} onClick={() => void runBulkAction('ENABLE')}>Enable</button>
				<button type="button" disabled={bulkWorking} onClick={() => void runBulkAction('DISABLE')}>Disable</button>
				<button className="danger-action" type="button" disabled={bulkWorking} onClick={() => void runBulkAction('DELETE')}>Delete</button>
			  </div>}
			</div>
		  )}
		  {bulkError && <div className="auth-error links-load-error" role="alert">{bulkError}</div>}

          {loading ? (
            <div className="links-state"><span className="health-dot" /> Loading secure links...</div>
          ) : links.content.length === 0 ? (
            <div className="dashboard-empty">
              <span className="capability-index">EMPTY / 00</span>
              <h2>{searchQuery || statusFilter ? 'No matching links.' : 'No links yet.'}</h2>
              <p>{searchQuery || statusFilter ? 'Try a different search term or status.' : 'Create your first short URL. Click events and analytics will appear here automatically.'}</p>
              {searchQuery || statusFilter
				? <button className="primary-button" type="button" onClick={clearFilters}>Clear filters <span>-&gt;</span></button>
				: <button className="primary-button" type="button" onClick={() => setShowCreate(true)}>Create a short link <span>-&gt;</span></button>}
            </div>
          ) : (
            <div className="links-list">
			  {links.content.map((link) => <LinkCard key={link.id} link={link} selected={selectedLinkIds.has(link.id)} onSelectionChanged={changeLinkSelection} onUpdated={handleUpdated} onDeleted={handleDeleted} onViewAnalytics={setSelectedAnalyticsLink} />)}
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
		{showAccountSettings && <AccountSettingsPanel user={user} onClose={() => setShowAccountSettings(false)} onPasswordChanged={onPasswordChanged} onProfileUpdated={onProfileUpdated} />}
      </section>
    </main>
  )
}
